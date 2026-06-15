package com.nan.waveform.vision.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/15 11:19
 *
 * 频率计算
 */

@Data
public class FrequencyIdentifyResultVo {
    private String fileName;                 // 原始图片文件名
    private List<FrequencyPhaseVo> phases;   // 三相频率测算矩阵

    @Data
    public static class FrequencyPhaseVo {
        private String phase;                // 相别：A / B / C
        private Double freqHz;               // 逼近后的工频频率 (Hz)
        private Double periodMs;             // 自相关锁定的周期 (ms)
        private String error;                // 异常隔离信息
    }
}
