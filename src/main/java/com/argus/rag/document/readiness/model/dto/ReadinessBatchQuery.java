package com.argus.rag.document.readiness.model.dto;

public class ReadinessBatchQuery {
    private Long groupId;
    private String status;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
