package com.nan.waveform.comtrade.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/11 15:44
 *
 * 存放解析出来的 CFG+DAT 大一统数据结果
 * 录波文件解析引擎的统一返回载体
 */

@Data
public class ComtradeParseResultDto {
    // 1. 全局配置信息
    private String stationName;
    private String deviceId;
    private String revYear;
    private double lineFrequency;
    private double timeMultiplier;
    private String startTime;
    private String triggerTime;

    // 2. 通道集合
    private List<AnalogChannelDto> analogChannels = new ArrayList<>();
    private List<DigitalChannelDto> digitalChannels = new ArrayList<>();

    // 3. 采样点还原数据集合
    private List<SamplePointDto> samplePoints = new ArrayList<>();
}
