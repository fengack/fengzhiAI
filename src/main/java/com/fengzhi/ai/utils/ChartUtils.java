package com.fengzhi.ai.utils;


import com.fengzhi.ai.common.ErrorCode;
import com.fengzhi.ai.exception.ThrowUtils;
import com.fengzhi.ai.manager.AiManager;
import com.fengzhi.ai.model.dto.chart.ChartGenResult;
import lombok.extern.slf4j.Slf4j;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

@Slf4j
public class ChartUtils {
    private static final String jsCodePrefix = "<ReactECharts option={";

    private static final String jsCodeSuffix ="}/>";
    /**
     * 获取 AI 生成结果
     * @param aiManager  AI 能力
     * @param goal
     * @param cvsData
     * @param chartType
     * @return
     */
    public static ChartGenResult getGenResult(final AiManager aiManager, final String goal, final String cvsData, final String chartType) {
        String promote = AiManager.PRECONDITION + "\n分析需求: \n" + goal + " \n原始数据如下: \n" + cvsData + "生成图表的类型是: " + chartType;
        String resultData = aiManager.doChat(promote);
        log.info("AI 生成的信息: {}", resultData);
        ThrowUtils.throwIf(resultData.split("'【【【【【'").length < 3, ErrorCode.SYSTEM_ERROR);
        String genChart = resultData.split("'【【【【【'")[1].trim();
        String genResult = resultData.split("'【【【【【'")[2].trim();
        return new ChartGenResult(genChart, genResult);
    }

    public static String replaceJson(String jsonString){
        return jsonString.replace("'","\"");
    }

    /**
     * mozilla 校验生成的Excharts代码
     * @param echartsCode
     * @return
     */

    public  static boolean checkEchartsTest(String echartsCode) {
        StringBuffer stringBuffer = new StringBuffer();
        String jsCode = stringBuffer.append(jsCodePrefix).append(echartsCode).append(jsCodeSuffix).toString();
        Context context = Context.enter();
        context.setErrorReporter(new ErrorReporter() {
            @Override
            public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
                System.err.println("Echarts code Warning: " + message);
            }

            @Override
            public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
                System.err.println("Echarts code Error: " + message);
            }

            @Override
            public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
                System.err.println("Echarts code Runtime Error: " + message);
                return new EvaluatorException(message);
            }
        });
        try {
            context.evaluateString(context.initStandardObjects(), jsCode, "JavaScriptCode", 1, null);
            return true;
        } catch (EvaluatorException e) {
            return false;
        } finally {
            Context.exit();
        }
    }
}
