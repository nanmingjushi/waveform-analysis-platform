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
        renderMap.put("volHarmonicConclusion", "2～25次谐波电压中均满足国标要求；");
        renderMap.put("curHarmonicConclusion", "2～25次谐波电流中均满足国标要求；");

        if (data.getDeviationUp() != null) {
            double ab = parse(data.getDeviationUp().getAbMax());
            double bc = parse(data.getDeviationUp().getBcMax());
            double ac = parse(data.getDeviationUp().getAcMax());
            double maxDev = Math.max(ab, Math.max(bc, ac));
            renderMap.put("volDevConclusion", String.format("电压偏差：0.00%%～%.2f%%（最大值）满足国标要求；", maxDev));
            renderMap.put("maxVoltageDeviation", String.format("%.2f", maxDev));
        } else {
            renderMap.put("volDevConclusion", "电压偏差：0.00%%～0.00%%（最大值）满足国标要求；");
            renderMap.put("maxVoltageDeviation", "0.00");
        }

        if (data.getFlickerAB() != null) {
            renderMap.put("flickerConclusion", String.format("长时间闪变（Plt）：%s（95%%值），满足国标要求；", data.getFlickerAB().getVal95()));
        }

        if (data.getUnbalance() != null) {
            renderMap.put("unbalanceConclusion", String.format("三相电压不平衡度：%s%%（95%%值），满足国标要求；", data.getUnbalance().getVal95()));
        }

        if (data.getFrequency() != null) {
            double maxFreq = parse(data.getFrequency().getMaxVal()) - 50.0;
            renderMap.put("freqConclusion", String.format("频率偏差：±%.2fHz，满足国标要求。", Math.abs(maxFreq)));
        }
    }

    private static double parse(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        try { return Double.parseDouble(val.trim()); } catch (Exception e) { return 0.0; }
    }
}
