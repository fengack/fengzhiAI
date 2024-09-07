package com.fengzhi.ai.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

/**
 * @version 1.0
 **/
@Data
public class BiResponse implements Serializable {
    //生成的图表代码
    private String genChart;
    //生成的图表分析结果
    private String genResult;
    //图表id
    private Long chartId;
}
