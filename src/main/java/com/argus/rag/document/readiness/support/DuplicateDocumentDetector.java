package com.argus.rag.document.readiness.support;

import com.argus.rag.document.mapper.DocumentMapper;
import com.argus.rag.document.model.entity.DocumentEntity;
import org.springframework.stereotype.Component;

@Component
public class DuplicateDocumentDetector {
    private final DocumentMapper documentMapper;

    public DuplicateDocumentDetector(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    public DocumentEntity findDuplicate(Long groupId, String fileHash) {
        if (groupId == null || fileHash == null || fileHash.isBlank()) {
            return null;
        }
        return documentMapper.selectByGroupIdAndFileHash(groupId, fileHash);
    }
}
