package com.nan.waveform.quality.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/16 11:28
 *
 * 装载全部结果
 */

@Data
public class PowerQualityDataDto {

    // 谐波表格集合
    private List<HarmonicRowDto> volHarmonics = new ArrayList<>();
    private List<HarmonicRowDto> curHarmonics = new ArrayList<>();


    private SteadyRowDto voltageDeviation; // 电压偏差
    private SteadyRowDto flicker;          // 长时间闪变
    private SteadyRowDto frequency;        // 频率
    private SteadyRowDto unbalance;        // 三相电压不平衡
}
