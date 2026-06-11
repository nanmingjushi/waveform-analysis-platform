package com.nan.waveform.comtrade.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/11 15:45
 *
 * 单个采样点数据模型
 */

@Data
public class SamplePointDto {
    private final int n;                // 采样点序号
    private final long timestampUs;     // 时间戳(微秒)

    // 存放该时刻所有模拟通道的还原值
    private final List<Double> analogValues = new ArrayList<>();
    // 存放该时刻所有数字通道的状态值(0或1)
    private final List<Integer> digitalValues = new ArrayList<>();

    public SamplePointDto(int n, long timestampUs) {
        this.n = n;
        this.timestampUs = timestampUs;
    }

    public void addAnalogValue(double value) {
        analogValues.add(value);
    }

    public void addDigitalValue(int value) {
        digitalValues.add(value);
    }
}
