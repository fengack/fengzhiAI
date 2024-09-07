package com.fengzhi.ai.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class BiVO {
    private String genChart;
    private String genResult;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long chartId;
}
