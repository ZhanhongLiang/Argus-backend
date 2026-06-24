package com.argus.rag.qa.service;

import com.argus.rag.auth.CurrentUserService;
import com.argus.rag.common.enums.SystemRole;
import com.argus.rag.common.exception.BusinessException;
import com.argus.rag.common.exception.ForbiddenException;
import com.argus.rag.group.mapper.GroupMembershipMapper;
import com.argus.rag.qa.model.QaRecordScope;
import com.argus.rag.qa.model.vo.QaScopeOptionVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Resolves QA record governance scopes and validates permissions. */
@Service
public class QaRecordScopeResolver {

    private final CurrentUserService currentUserService;
    private final GroupMembershipMapper groupMembershipMapper;

    public QaRecordScopeResolver(CurrentUserService currentUserService,
                                 GroupMembershipMapper groupMembershipMapper) {
        this.currentUserService = currentUserService;
        this.groupMembershipMapper = groupMembershipMapper;
    }

    public ScopeContext resolve(String rawScope, Long groupId) {
        CurrentUserService.CurrentUser currentUser = currentUserService.getRequiredCurrentUser();
        boolean systemAdmin = currentUser.systemRole() == SystemRole.ADMIN;
        QaRecordScope scope = parseScope(rawScope);

        if (scope == QaRecordScope.GLOBAL) {
            if (!systemAdmin) {
                throw new ForbiddenException("GLOBAL scope requires system ADMIN");
            }
            return new ScopeContext(scope, currentUser.userId(), null, true);
        }

        if (scope == QaRecordScope.GROUP) {
            if (groupId == null || groupId <= 0) {
                throw new BusinessException("GROUP scope requires groupId");
            }
            if (!systemAdmin) {
                String role = groupMembershipMapper.selectActiveMembershipRole(currentUser.userId(), groupId);
                if (!"OWNER".equals(role)) {
                    throw new ForbiddenException("GROUP scope requires group OWNER or system ADMIN");
                }
            }
            return new ScopeContext(scope, currentUser.userId(), groupId, systemAdmin);
        }

        return new ScopeContext(QaRecordScope.SELF, currentUser.userId(), groupId, systemAdmin);
    }

    public List<QaScopeOptionVO> listScopeOptions() {
        CurrentUserService.CurrentUser currentUser = currentUserService.getRequiredCurrentUser();
        List<QaScopeOptionVO> options = new ArrayList<>();
        options.add(new QaScopeOptionVO("SELF", null, "我的记录"));

        List<Map<String, Object>> ownedGroups = groupMembershipMapper.selectOwnedGroupsByUserId(currentUser.userId());
        for (Map<String, Object> group : ownedGroups) {
            Object id = group.get("groupId");
            Long groupId = id instanceof Number number ? number.longValue() : null;
            String groupName = String.valueOf(group.getOrDefault("groupName", "知识库"));
            options.add(new QaScopeOptionVO("GROUP", groupId, groupName));
        }

        if (currentUser.systemRole() == SystemRole.ADMIN) {
            options.add(new QaScopeOptionVO("GLOBAL", null, "全局"));
        }
        return options;
    }

    private QaRecordScope parseScope(String rawScope) {
        if (!StringUtils.hasText(rawScope)) {
            return QaRecordScope.SELF;
        }
        try {
            return QaRecordScope.valueOf(rawScope.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("QA record scope is invalid");
        }
    }

    public record ScopeContext(
            QaRecordScope scope,
            Long currentUserId,
            Long groupId,
            boolean systemAdmin
    ) {
    }
}
