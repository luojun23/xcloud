package com.njtech.xcloud.dto;

import java.io.Serializable;

public class AnalysisTaskMsg implements Serializable {
    private String fileId;
    private String action;

    public AnalysisTaskMsg() {}

    public AnalysisTaskMsg(String fileId, String action) {
        this.fileId = fileId;
        this.action = action;
    }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
