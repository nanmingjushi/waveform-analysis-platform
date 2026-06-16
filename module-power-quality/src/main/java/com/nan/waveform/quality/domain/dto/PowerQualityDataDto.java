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



    private SteadyRowDto frequency;
    private SteadyRowDto unbalance;
    private SteadyRowDto flickerAB;
    private SteadyRowDto flickerBC;
    private SteadyRowDto flickerAC;


    private DeviationRowDto deviationUp;
    private DeviationRowDto deviationDown;
}
