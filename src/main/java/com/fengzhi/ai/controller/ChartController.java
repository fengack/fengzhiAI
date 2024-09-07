package com.fengzhi.ai.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;

import com.fengzhi.ai.common.*;
import com.fengzhi.ai.constant.CommonConstant;

import com.fengzhi.ai.constant.MqConstant;
import com.fengzhi.ai.constant.UserConstant;
import com.fengzhi.ai.exception.BusinessException;
import com.fengzhi.ai.exception.ThrowUtils;
import com.fengzhi.ai.manager.AiManager;
import com.fengzhi.ai.manager.CosManager;
import com.fengzhi.ai.manager.RedissonLimiterManager;
import com.fengzhi.ai.model.dto.chart.*;

import com.fengzhi.ai.model.entity.Chart;
import com.fengzhi.ai.model.entity.User;
import com.fengzhi.ai.model.enums.FileUploadBizEnum;
import com.fengzhi.ai.model.vo.BiVO;
import com.fengzhi.ai.service.ChartService;
import com.fengzhi.ai.service.UserService;
import com.fengzhi.ai.utils.ChartUtils;
import com.fengzhi.ai.utils.ExcelUtils;
import com.fengzhi.ai.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;


@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedissonLimiterManager redissonLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private RabbitTemplate rabbitTemplate;

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }


    /**
     * 智能分析
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiVO> genCharByAi(@RequestPart("file") MultipartFile multipartFile,
                                            GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        // 获取前端用户的请求数据
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 对数据进行校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name)&&name.length() > 100, ErrorCode.PARAMS_ERROR,"名称过长");
        // 校验文件大小
        long multipartFileSize = multipartFile.getSize();
        final long FILE_SIZE = 1024 * 1024L;
        ThrowUtils.throwIf(multipartFileSize > FILE_SIZE,ErrorCode.PARAMS_ERROR,"文件过大!");
        // 校验文件后缀
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> VAL_SUFFIX = Arrays.asList("xls","xlsx");
        ThrowUtils.throwIf(!VAL_SUFFIX.contains(suffix),ErrorCode.PARAMS_ERROR,"不支持此文件类型!");


        // 查看代码实现
        User loginUser = userService.getLoginUser(request);

        // 对用户使用redisson进行限流
        redissonLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        // 将用户传入的表格转换为csv
        String data = ExcelUtils.excelToCsv(multipartFile);

        // 导入ai接口生成所需数据
        ChartGenResult result = ChartUtils.getGenResult(aiManager, goal, data, chartType);
        String genChart = ChartUtils.replaceJson(result.getGenChart());

        // 保存生成的图表数据
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(data);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(result.getGenResult());
        chart.setUserId(loginUser.getId());
        chart.setStatus(ChartStatus.SUCCEED.getStatus());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"图表保存失败!");

        BiVO biVO = new BiVO();
        biVO.setGenChart(genChart);
        biVO.setGenResult(result.getGenResult());
        biVO.setChartId(chart.getId());

        return ResultUtils.success(biVO);
    }

    /**
     * 智能分析（异步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiVO> genCharByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                          GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        // 获取前端用户的请求数据
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 对数据进行校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name)&&name.length() > 100, ErrorCode.PARAMS_ERROR,"名称过长");
        // 校验文件大小
        long multipartFileSize = multipartFile.getSize();
        final long FILE_SIZE = 1024 * 1024L;
        ThrowUtils.throwIf(multipartFileSize > FILE_SIZE,ErrorCode.PARAMS_ERROR,"文件过大!");
        // 校验文件后缀
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> VAL_SUFFIX = Arrays.asList("xls","xlsx");
        ThrowUtils.throwIf(!VAL_SUFFIX.contains(suffix),ErrorCode.PARAMS_ERROR,"不支持此文件类型!");


        // 查看代码实现
        User loginUser = userService.getLoginUser(request);

        // 对用户使用redisson进行限流
        redissonLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        // 将用户传入的表格转换为csv
        String data = ExcelUtils.excelToCsv(multipartFile);


        // 保存生成的图表数据
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(data);
        chart.setChartType(chartType);
        chart.setStatus(ChartStatus.WAIT.getStatus());
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"图表保存失败!");

        CompletableFuture.runAsync(()->{
            // 将图表状态更新为正在生成
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus(String.valueOf(ChartStatus.RUNNING.getStatus()));
            boolean update = chartService.updateById(updateChart);
            if(!update){
                handleChartUpdateError(chart.getId(), "更新图表失败!");
            }
            // 导入ai接口生成所需数据
            ChartGenResult result = ChartUtils.getGenResult(aiManager, goal, data, chartType);
            String genChart = ChartUtils.replaceJson(result.getGenChart());

            // 更新生成的图表
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(result.getGenChart());
            updateChartResult.setStatus(String.valueOf(ChartStatus.SUCCEED.getStatus()));
            boolean updateResult = chartService.updateById(updateChartResult);
            if (!updateResult) {
                handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
            }

        },threadPoolExecutor);

        BiVO biVO = new BiVO();
        biVO.setChartId(chart.getId());

        return ResultUtils.success(biVO);
    }

    @PostMapping("/gen/async/mq")
    public BaseResponse<BiVO> genCharByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                               GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        // 获取前端用户的请求数据
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 对数据进行校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name)&&name.length() > 100, ErrorCode.PARAMS_ERROR,"名称过长");
        // 校验文件大小
        long multipartFileSize = multipartFile.getSize();
        final long FILE_SIZE = 1024 * 1024L;
        ThrowUtils.throwIf(multipartFileSize > FILE_SIZE,ErrorCode.PARAMS_ERROR,"文件过大!");
        // 校验文件后缀
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> VAL_SUFFIX = Arrays.asList("xls","xlsx");
        ThrowUtils.throwIf(!VAL_SUFFIX.contains(suffix),ErrorCode.PARAMS_ERROR,"不支持此文件类型!");


        // 查看代码实现
        User loginUser = userService.getLoginUser(request);

        // 对用户使用redisson进行限流
        redissonLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        // 将用户传入的表格转换为csv
        String data = ExcelUtils.excelToCsv(multipartFile);


        // 保存生成的图表数据
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(data);
        chart.setChartType(chartType);
        chart.setStatus(ChartStatus.WAIT.getStatus());
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"图表保存失败!");
        // 获取保存后的图表Id
        Long charId = chart.getId();
        // 使用RabbitMQ发送消息给exchange
        rabbitTemplate.convertAndSend(MqConstant.EXCHANGE_NAME,MqConstant.BI_ROUTING_KEY,String.valueOf(charId));

        BiVO biVO = new BiVO();
        biVO.setChartId(charId);

        return ResultUtils.success(biVO);
    }

    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus(ChartStatus.FAILED.getStatus());
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }

    /**
     * 根据 id 获取
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }


    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion
    @PostMapping("/gen/retry")
//    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<BiResponse> retryGenChart(@RequestBody final ChartRetryRequest chartRegenRequest,HttpServletRequest httpServletRequest){
        //参数检验
        ThrowUtils.throwIf(chartRegenRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        Long chartId= chartRegenRequest.getId();
        String name = chartRegenRequest.getName();
        String goal = chartRegenRequest.getGoal();
        String chartData = chartRegenRequest.getChartData();
        String chartType = chartRegenRequest.getChartType();
        ThrowUtils.throwIf(chartId == null || chartId <= 0, ErrorCode.PARAMS_ERROR, "图表不存在");
        ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR, "图表名称为空");
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        ThrowUtils.throwIf(StringUtils.isBlank(chartData), ErrorCode.PARAMS_ERROR, "原始数据为空");
        ThrowUtils.throwIf(StringUtils.isBlank(chartType), ErrorCode.PARAMS_ERROR, "图表类型为空");
        //限流
        redissonLimiterManager.doRateLimit("genChartByAi_"+loginUser.getId());
        //将修改后的数据填到chart中
        Chart chart = new Chart();
        chart.setId(chartId);
        chart.setChartType(chartType);
        chart.setChartData(chartData);
        chart.setName(name);
        chart.setGoal(goal);
        chart.setStatus(ChartStatus.WAIT.getStatus());

        //更新数据库
        chartService.updateById(chart);
        //将chartid发送到消息队列
        rabbitTemplate.convertAndSend(MqConstant.EXCHANGE_NAME,MqConstant.BI_ROUTING_KEY, String.valueOf(chartId));

        BiResponse biResponse=new BiResponse();
        biResponse.setChartId(chartId);
        return ResultUtils.success(biResponse) ;
    }

    /**
     * 获取查询包装类
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


}
