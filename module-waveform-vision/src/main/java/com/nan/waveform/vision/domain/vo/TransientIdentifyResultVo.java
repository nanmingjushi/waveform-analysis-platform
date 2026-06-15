package com.nan.waveform.vision.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/15 11:17
 *
 * 波形图像暂态最大值识别
 */

@Data
public class TransientIdentifyResultVo {
    private String fileName;                 // 原始图片文件名
    private List<TransientPhaseVo> phases;   // 三相识别明细矩阵

    @Data
    public static class TransientPhaseVo {
        private String phase;                // 相别：A / B / C
        private Double value;                // 换算后的物理量最大值（k级单位）
        private Integer waveTopY;            // 诊断像素：波形最高点 y 坐标
        private String error;                // 异常隔离信息（若单相识别失败）
    }
}
