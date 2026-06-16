package com.nan.waveform.quality.domain.dto;

import lombok.Data;

/**
 * @author nan chao
 * @since 2026/6/16 11:26
 * 频率偏差、三相电压不平衡度及长时间闪变统计表
 * 电压偏差统计表
 */

@Data
public class SteadyRowDto {
    private String itemName;   // 测量项目名称
    private String maxVal;     // 最大值
    private String avgVal;     // 平均值
    private String minVal;     // 最小值
    private String val95;      // 95%值
    private String limitVal;   // 限值
}
