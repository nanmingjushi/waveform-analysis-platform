package com.nan.waveform.vision.domain.vo;

import lombok.Data;

/**
 * @author nan chao
 * @since 2026/6/15 11:20
 *
 * 阶跃响应时间
 */

@Data
public class StepResponseResultVo {
    private String fileName;   // 原始图像文件名
    private Double t5;         // 阶跃前过渡交叉点时间 t5
    private Double t95;        // 阶跃后趋稳交叉点时间 t95
    private Double tStep;      // 最终无损测算出的阶跃时间差 tStep
    private String error;      // 智能熔断异常原因
}
