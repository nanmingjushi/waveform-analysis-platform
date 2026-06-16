package com.nan.waveform.quality.core.calculator;

import com.deepoove.poi.data.TextRenderData;
import com.deepoove.poi.data.Texts;

import java.util.Map;

/**
 * @author nan chao
 * @since 2026/6/16 10:25
 *
 * 合规判定富文本引擎
 */

public class ComplianceJudgeEngine {
    public static void injectComplianceConclusions(Map<String, Object> map) {
        // 核心逻辑：比对 95% 值与国标限值
        boolean isVoltagePass = true; // 实际逻辑需从 map 里遍历比对

        if (isVoltagePass) {
            // 合格，给 Word 模板推入正常黑色文字
            map.put("voltageConclusion", Texts.of("满足国标要求").create());
        } else {
            // 利用 poi-tl 富文本渲染，在 Word 里直接高亮标红加粗
            TextRenderData redWarning = Texts.of("不满足国标要求")
                    .color("FF0000") // 纯红色
                    .bold()
                    .create();
            map.put("voltageConclusion", redWarning);
        }

        // 其他参数（频率、不平衡度等）依此类推...
    }
}
