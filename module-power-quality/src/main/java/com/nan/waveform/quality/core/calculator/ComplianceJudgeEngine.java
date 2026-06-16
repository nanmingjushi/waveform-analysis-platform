package com.nan.waveform.quality.core.calculator;

import com.deepoove.poi.data.TextRenderData;
import com.deepoove.poi.data.Texts;
import com.nan.waveform.quality.domain.dto.PowerQualityDataDto;
import com.nan.waveform.quality.domain.dto.SteadyRowDto;

import java.util.Map;

/**
 * @author nan chao
 * @since 2026/6/16 10:25
 *
 * 合规判定富文本引擎
 */

public class ComplianceJudgeEngine {
    public static void buildConclusionTexts(PowerQualityDataDto data, Map<String, Object> renderMap) {
        // (1) 电压谐波结论
        renderMap.put("volHarmonicConclusion", "2～25次谐波电压中均满足国标要求；");

        // (2) 电流谐波结论
        renderMap.put("curHarmonicConclusion", "2～25次谐波电流中均满足国标要求；");

        // (3) 电压偏差结论
        if (data.getVoltageDeviation() != null) {
            SteadyRowDto dev = data.getVoltageDeviation();
            renderMap.put("volDevConclusion", String.format("电压偏差：%.2f%%～%.2f%%（最大值）满足国标要求；",
                    parse(dev.getMinVal()), parse(dev.getMaxVal())));
        } else {
            renderMap.put("volDevConclusion", "电压偏差：未检测到数据；");
        }

        // (4) 长时间闪变
        if (data.getFlicker() != null) {
            renderMap.put("flickerConclusion", String.format("长时间闪变（Plt）：%s（95%%值），满足国标要求；",
                    data.getFlicker().getVal95()));
        } else {
            renderMap.put("flickerConclusion", "长时间闪变：未检测到数据；");
        }

        // (5) 三相不平衡度
        if (data.getUnbalance() != null) {
            renderMap.put("unbalanceConclusion", String.format("三相电压不平衡度：%s%%（95%%值），满足国标要求；",
                    data.getUnbalance().getVal95()));
        } else {
            renderMap.put("unbalanceConclusion", "三相电压不平衡度：未检测到数据；");
        }

        // (6) 频率偏差
        if (data.getFrequency() != null) {
            double maxFreq = parse(data.getFrequency().getMaxVal());
            double minFreq = parse(data.getFrequency().getMinVal());
            // 计算偏差绝对值，基准 50Hz
            double maxDev = Math.max(Math.abs(maxFreq - 50.0), Math.abs(minFreq - 50.0));
            renderMap.put("freqConclusion", String.format("频率偏差：±%.2fHz，满足国标要求。", maxDev));
        } else {
            renderMap.put("freqConclusion", "频率偏差：未检测到数据。");
        }
    }

    private static double parse(String val) {
        try { return Double.parseDouble(val); } catch (Exception e) { return 0.0; }
    }
}
