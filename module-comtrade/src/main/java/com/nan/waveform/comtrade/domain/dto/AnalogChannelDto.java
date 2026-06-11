package com.nan.waveform.comtrade.domain.dto;

import lombok.Data;

/**
 * @author nan chao
 * @since 2026/6/11 15:44
 *
 * 模拟通道数据模型
 */

@Data
public class AnalogChannelDto {
    private String an;      // 通道序号
    private String chId;    // 通道名称
    private String ph;      // 相位
    private String ccbm;    // 被监测元件
    private String uu;      // 单位 (V, A等)
    private double a;       // 增益倍数
    private double b;       // 偏移量
    private double skew;    // 时滞偏移
    private String min;     // 最小值
    private String max;     // 最大值
    private double primary; // 一次侧变比
    private double secondary; // 二次侧变比
    private String ps;      // P或S
}
