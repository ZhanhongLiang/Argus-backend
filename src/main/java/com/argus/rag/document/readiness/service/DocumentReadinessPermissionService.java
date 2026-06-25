package com.argus.rag.document.readiness.service;

import com.argus.rag.auth.CurrentUserService;
import com.argus.rag.common.enums.GroupRole;
import com.argus.rag.common.enums.SystemRole;
import com.argus.rag.common.exception.BusinessException;
import com.argus.rag.common.exception.ForbiddenException;
import com.argus.rag.group.mapper.GroupMembershipMapper;
import org.springframework.stereotype.Service;

@Service
public class DocumentReadinessPermissionService {
    private final CurrentUserService currentUserService;
    private final GroupMembershipMapper groupMembershipMapper;

    public DocumentReadinessPermissionService(CurrentUserService currentUserService,
                                              GroupMembershipMapper groupMembershipMapper) {
        this.currentUserService = currentUserService;
        this.groupMembershipMapper = groupMembershipMapper;
    }

    public CurrentUserService.CurrentUser currentUser() {
        return currentUserService.getRequiredCurrentUser();
    }

    public boolean isSystemAdmin(CurrentUserService.CurrentUser user) {
        return user.systemRole() == SystemRole.ADMIN;
    }

    public CurrentUserService.CurrentUser requireBatchCreator(Long groupId) {
        CurrentUserService.CurrentUser user = currentUser();
        if (isSystemAdmin(user)) {
            return user;
        }
        requireOwner(user.userId(), groupId);
        return user;
    }

    public CurrentUserService.CurrentUser requireMemberUploader(Long groupId) {
        CurrentUserService.CurrentUser user = currentUser();
        if (isSystemAdmin(user)) {
            return user;
        }
        requireMember(user.userId(), groupId);
        return user;
    }

    public CurrentUserService.CurrentUser requireManager(Long groupId) {
        CurrentUserService.CurrentUser user = currentUser();
        if (isSystemAdmin(user)) {
            return user;
        }
        requireOwner(user.userId(), groupId);
        return user;
    }

    public void requireReadable(Long groupId) {
        CurrentUserService.CurrentUser user = currentUser();
        if (isSystemAdmin(user)) {
            return;
        }
        requireMember(user.userId(), groupId);
    }

    private void requireOwner(Long userId, Long groupId) {
        String role = role(userId, groupId);
        if (!GroupRole.OWNER.name().equals(role)) {
            throw new ForbiddenException("当前用户不是目标群组 OWNER");
        }
    }

    private void requireMember(Long userId, Long groupId) {
        if (role(userId, groupId) == null) {
            throw new ForbiddenException("当前用户不是目标群组成员");
        }
    }

    private String role(Long userId, Long groupId) {
        if (groupId == null || groupId <= 0) {
            throw new BusinessException("groupId 非法");
        }
        return groupMembershipMapper.selectActiveMembershipRole(userId, groupId);
    }
}
