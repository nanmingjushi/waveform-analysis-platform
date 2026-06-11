package com.nan.waveform.comtrade.core.parser;

import com.nan.waveform.comtrade.domain.dto.AnalogChannelDto;
import com.nan.waveform.comtrade.domain.dto.ComtradeParseResultDto;
import com.nan.waveform.comtrade.domain.dto.SamplePointDto;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/11 15:40
 * 解析 .dat 二进制流，结合 Cfg 数据还原波形采样点
 */

@Slf4j
public class DatParser {
    /**
     * 解析 DAT 二进制输入流并将其还原的数据注入到大一统 DTO 对象中
     * @param inputStream DAT 文件的输入流
     * @param cfgResult   已经解析好的 CFG 配置载体 (算法强依赖其中的通道参数)
     */
    public static void parse(InputStream inputStream, ComtradeParseResultDto cfgResult) throws IOException {
        // 1. 将输入流读入内存字节数组 (方便指针动态移动解包)
        byte[] data = readAllBytes(inputStream);
        int dataLen = data.length;

        int analogNum = cfgResult.getAnalogChannels().size();
        int digitalNum = cfgResult.getDigitalChannels().size();
        double timeMult = cfgResult.getTimeMultiplier();
        List<AnalogChannelDto> analogChannels = cfgResult.getAnalogChannels();

        // 2. 根据标准协议计算单条数据记录的字节打包长度 (核心公式)
        // 4字节(序号) + 4字节(时间戳) + 模拟量(每个2字节) + 数字量(每16个通道占用2字节)
        int datPackLen = 4 + 4 + (analogNum * 2) + (int) Math.ceil(digitalNum / 16.0) * 2;

        if (dataLen % datPackLen != 0) {
            log.warn("警告：DAT文件总字节数 [{}] 与计算出的单包长度 [{}] 不成正比，可能文件数据不完整", dataLen, datPackLen);
        }

        List<SamplePointDto> samplePoints = new ArrayList<>();

        // 3. 循环步进解包，每次移动一个数据包的长度
        for (int pos = 0; pos < dataLen; pos += datPackLen) {
            // 如果最后一包数据由于文件截断导致不完整，防止越界直接跳出
            if (pos + datPackLen > dataLen) {
                break;
            }
            SamplePointDto samplePoint = unpackSinglePacket(data, pos, analogNum, digitalNum, timeMult, analogChannels);
            samplePoints.add(samplePoint);
        }

        // 4. 将最终解析还原的采样点集合注入大一统载体
        cfgResult.setSamplePoints(samplePoints);
        log.info("DAT二进制文件解包成功! 共计解析出 [{}] 个时间维度的采样点记录", samplePoints.size());
    }

    /**
     * 解包单个采样点记录的数据包 (纯内部私有方法，保证封装性)
     */
    private static SamplePointDto unpackSinglePacket(byte[] data, int pos, int analogNum, int digitalNum,
                                                     double timeMult, List<AnalogChannelDto> analogChannels) {
        // 使用 ByteBuffer 包装当前字节片段，并强制指定为电力标准的 LITTLE_ENDIAN (小端序)
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // 1. 提取采样编号 (4字节)
        int n = buffer.getInt(pos);

        // 2. 提取原始时间标记 (4字节) 并应用倍乘系数
        int timestampRaw = buffer.getInt(pos + 4);
        long timestampUs = (timestampRaw >= 0x80000000) ? -((timestampRaw ^ 0xFFFFFFFF) + 1) : timestampRaw;
        timestampUs *= timeMult;

        SamplePointDto samplePoint = new SamplePointDto(n, timestampUs);

        // 3. 解析模拟通道数据 (每个通道2字节的 short 原始值)
        for (int i = 0; i < analogNum; i++) {
            short analogRaw = buffer.getShort(pos + 8 + (i * 2));
            AnalogChannelDto channel = analogChannels.get(i);

            // 物理量还原第一步：原始值 * 增益 a + 偏移 b
            double adjustedValue = analogRaw * channel.getA() + channel.getB();

            // 物理量还原第二步：如果配置标识为 P (一次侧)，需要进一步乘以互感器一/二次侧变比比率
            if ("P".equalsIgnoreCase(channel.getPs())) {
                if (channel.getSecondary() != 0) { // 强健壮性：规避除以0的极端异常
                    adjustedValue *= (channel.getPrimary() / channel.getSecondary());
                }
            }
            samplePoint.addAnalogValue(adjustedValue);
        }

        // 4. 解析数字通道状态数据 (每16个通道打包成一个2字节的 short 状态块)
        for (int i = 0; i < digitalNum; i++) {
            short digitalChunk = buffer.getShort(pos + 8 + (analogNum * 2) + (i / 16) * 2);
            int bitOffset = i % 16;
            // 通过位移和与运算抽取特定位的值 (0 或 1)
            int digitalValue = (digitalChunk >> (15 - bitOffset)) & 1;
            samplePoint.addDigitalValue(digitalValue);
        }

        return samplePoint;
    }

    /**
     * 内部兼容性工具方法：将全局 InputStream 转换为方便指针切片的 byte 数组
     */
    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }
}
