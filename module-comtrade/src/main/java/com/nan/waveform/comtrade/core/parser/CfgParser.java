package com.nan.waveform.comtrade.core.parser;

import com.nan.waveform.comtrade.domain.dto.AnalogChannelDto;
import com.nan.waveform.comtrade.domain.dto.ComtradeParseResultDto;
import com.nan.waveform.comtrade.domain.dto.DigitalChannelDto;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/11 15:40
 * 解析 .cfg 文件，提取通道元数据
 */

@Slf4j
public class CfgParser {
    /**
     * 解析 CFG 文件输入流
     * @param inputStream CFG文件的输入流
     * @return 填充了通道配置的 ComtradeParseResultDto 对象
     */
    public static ComtradeParseResultDto parse(InputStream inputStream) throws IOException {
        // 每次请求都创建独立的 Result 载体，绝不共享，保证线程安全
        ComtradeParseResultDto result = new ComtradeParseResultDto();

        List<AnalogChannelDto> analogChannels = new ArrayList<>();
        List<DigitalChannelDto> digitalChannels = new ArrayList<>();
        List<Double> samplingFrequencies = new ArrayList<>();
        List<Integer> endsamps = new ArrayList<>();

        // 电力系统标准的录波文件通常采用 GBK 编码（防止中文厂站名或通道名乱码）
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "GBK"))) {
            String line;
            int lineNumber = 0;
            int analogNum = 0;
            int digitalNum = 0;
            int nrates = 0;

            while ((line = reader.readLine()) != null) {
                // 去除两端空格并按逗号切分
                String[] parts = line.trim().split(",");

                if (lineNumber == 0) {
                    // 行 0：厂站名称, 记录标识, 版本年号
                    result.setStationName(parts.length > 0 ? parts[0].trim() : "");
                    result.setDeviceId(parts.length > 1 ? parts[1].trim() : "");
                    result.setRevYear(parts.length > 2 ? parts[2].trim() : "");

                } else if (lineNumber == 1) {
                    // 行 1：通道总数, 模拟通道数(以A结尾), 数字通道数(以D结尾)
                    if (parts.length >= 3) {
                        analogNum = Integer.parseInt(parts[1].toUpperCase().replace("A", "").trim());
                        digitalNum = Integer.parseInt(parts[2].toUpperCase().replace("D", "").trim());
                    }

                } else if (lineNumber >= 2 && lineNumber < 2 + analogNum) {
                    // 模拟通道解析区
                    AnalogChannelDto channel = new AnalogChannelDto();
                    channel.setAn(parts[0].trim());
                    channel.setChId(parts[1].trim());
                    channel.setPh(parts[2].trim());
                    channel.setCcbm(parts[3].trim());
                    channel.setUu(parts[4].trim());
                    channel.setA(Double.parseDouble(parts[5].trim()));
                    channel.setB(Double.parseDouble(parts[6].trim()));
                    channel.setSkew(Double.parseDouble(parts[7].trim()));
                    channel.setMin(parts[8].trim());
                    channel.setMax(parts[9].trim());
                    channel.setPrimary(Double.parseDouble(parts[10].trim()));
                    channel.setSecondary(Double.parseDouble(parts[11].trim()));
                    channel.setPs(parts[12].trim());
                    analogChannels.add(channel);

                } else if (lineNumber >= 2 + analogNum && lineNumber < 2 + analogNum + digitalNum) {
                    // 数字通道解析区
                    DigitalChannelDto channel = new DigitalChannelDto();
                    channel.setDn(parts[0].trim());
                    channel.setChId(parts[1].trim());
                    channel.setPh(parts[2].trim());
                    channel.setCcbm(parts[3].trim());
                    channel.setY(parts[4].trim());
                    digitalChannels.add(channel);

                } else if (lineNumber == 2 + analogNum + digitalNum) {
                    // 名义线路频率
                    result.setLineFrequency(Double.parseDouble(parts[0].trim()));

                } else if (lineNumber == 3 + analogNum + digitalNum) {
                    // 采样频率个数
                    nrates = Integer.parseInt(parts[0].trim());

                } else if (lineNumber >= 4 + analogNum + digitalNum && lineNumber < 4 + analogNum + digitalNum + nrates) {
                    // 采样频率与对应的最后采样点
                    samplingFrequencies.add(Double.parseDouble(parts[0].trim()));
                    endsamps.add(Integer.parseInt(parts[1].trim()));

                } else if (lineNumber == 4 + analogNum + digitalNum + nrates) {
                    // 第一个采样点的日期和时间 (起始时间戳)
                    result.setStartTime(parts[0].trim() + " " + parts[1].trim());

                } else if (lineNumber == 5 + analogNum + digitalNum + nrates) {
                    // 触发点的日期和时间
                    result.setTriggerTime(parts[0].trim() + " " + parts[1].trim());

                } else if (lineNumber == 7 + analogNum + digitalNum + nrates) {
                    // 时间标记倍乘系数
                    result.setTimeMultiplier(Double.parseDouble(parts[0].trim()));
                }

                lineNumber++;
            }
        }

        // 将解析好的通道集合注入大一统结果对象
        result.setAnalogChannels(analogChannels);
        result.setDigitalChannels(digitalChannels);

        log.info("CFG文本配置文件解析引擎运行成功! 厂站: [{}], 识别到模拟通道数: [{}], 数字通道数: [{}]",
                result.getStationName(), analogChannels.size(), digitalChannels.size());

        return result;
    }

}
