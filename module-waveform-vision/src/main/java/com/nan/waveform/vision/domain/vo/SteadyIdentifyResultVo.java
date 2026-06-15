package com.nan.waveform.vision.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/15 11:19
 *
 * 波形图像稳态值识别
 */

@Data
public class SteadyIdentifyResultVo {
    private String fileName;              // 原始图片文件名
    private String mode;                  // 识别模式：voltage / current
    private String unit;                  // 物理单位：V / A / kV
    private List<SteadyPhaseVo> phases;   // 三相稳态数据矩阵

    @Data
    public static class SteadyPhaseVo {
        private String phase;             // 相别：A / B / C
        private Double steadyPeakV;       // 稳态峰值
        private Double steadyRmsV;        // 理论RMS有效值
        private Double sampleRmsV;        // 全采样窗口测算RMS有效值
        private String error;             // 异常隔离信息
    }
}
