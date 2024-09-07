package com.fengzhi.ai.bimq;


import cn.hutool.core.bean.BeanUtil;
import com.rabbitmq.client.Channel;
import com.fengzhi.ai.common.ChartStatus;
import com.fengzhi.ai.common.ErrorCode;
import com.fengzhi.ai.constant.MqConstant;
import com.fengzhi.ai.exception.BusinessException;
import com.fengzhi.ai.manager.AiManager;
import com.fengzhi.ai.model.dto.chart.ChartGenResult;
import com.fengzhi.ai.model.entity.Chart;
import com.fengzhi.ai.service.ChartService;
import com.fengzhi.ai.utils.ChartUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
@Slf4j
public class BiMqComsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstant.QUEUE_NAME),
            exchange = @Exchange(name = MqConstant.EXCHANGE_NAME, type = ExchangeTypes.DIRECT),
            key = MqConstant.BI_ROUTING_KEY))
    public void receiveMessage(String message, Channel channel,@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("receiveMessage message = {}", message);
        if(StringUtils.isBlank(message)){
            throwExceptionAndNackMessage(channel,deliveryTag,"消息为空!");
        }

        // 查询图表信息
        Chart chart = chartService.getById(Long.parseLong(message));
        if(BeanUtil.isEmpty(chart)){
            throwExceptionAndNackMessage(channel,deliveryTag,"图表查询失败!");
        }

        // 将图表状态更新为正在生成
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus(String.valueOf(ChartStatus.RUNNING.getStatus()));
        boolean update = chartService.updateById(updateChart);

        if(!update){
            handleChartUpdateError(chart.getId(), "更新图表失败!");
        }

        // 使用SparkAi生成Echarts js 代码和分析结果
        ChartGenResult result = ChartUtils.getGenResult(aiManager,chart.getGoal(),chart.getChartData(),chart.getChartType());

        String genChart = ChartUtils.replaceJson(result.getGenChart());

        // 更新生成的图表
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(result.getGenResult());
        updateChartResult.setStatus(String.valueOf(ChartStatus.SUCCEED.getStatus()));
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
        }
        channel.basicAck(deliveryTag,false);
    }

    /**
     * 处理图表更新错误
     * @param chartId
     */
    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus(ChartStatus.FAILED.getStatus());
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }
    private void throwExceptionAndNackMessage(Channel channel, long deliveryTag,String errorMessage) {
        try {
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR,errorMessage);
    }
}
