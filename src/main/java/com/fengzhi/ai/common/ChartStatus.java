package com.fengzhi.ai.common;

/**
 * 生成图表状态
 */
public enum ChartStatus {
    WAIT("wait"),
    RUNNING("running"),
    SUCCEED("succeed"),
    FAILED("failed");
    private final String status;


    ChartStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

}
