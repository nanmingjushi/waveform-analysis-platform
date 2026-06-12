package com.nan.waveform.comtrade.core.exporter;

import com.nan.waveform.comtrade.domain.dto.AnalogChannelDto;
import com.nan.waveform.comtrade.domain.dto.ComtradeParseResultDto;
import com.nan.waveform.comtrade.domain.dto.DigitalChannelDto;
import com.nan.waveform.comtrade.domain.dto.SamplePointDto;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/11 15:41
 *
 * 将解析出的采样点集合转换为 CSV 字节流，供用户下载
 */

@Slf4j
public class CsvExporter {
    /**
     * 将解析出的大一统解析结果转换为标准的 CSV 数据并写入到输出流中
     * @param outputStream 目标输出流 (可以是网络下载流，也可以是本地文件写入流)
     * @param parseResult  解析出的结构化录波数据大对象
     */
    public static void exportToCsv(OutputStream outputStream, ComtradeParseResultDto parseResult) throws IOException {
        List<AnalogChannelDto> analogChannels = parseResult.getAnalogChannels();
        List<DigitalChannelDto> digitalChannels = parseResult.getDigitalChannels();
        List<SamplePointDto> samplePoints = parseResult.getSamplePoints();

        // 引入高精度数字格式化器，强制保留小数点后 6 位
        // 彻底粉碎 Java 在计算 原始值 * A + B 时由于二进制转换产生的诸如 .000000000001 这样的低级精度噪声
        DecimalFormat df = new DecimalFormat("#.######");


        // 使用 BufferedWriter 并强制指定为电力系统通用的 GBK 编码（保证用户用 Excel 直接双击打开 CSV 时中文不乱码）
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "GBK"))) {

            // 1. 动态构建并写入 CSV 表头 (Header)
            StringBuilder header = new StringBuilder();
            header.append("n,timestamp_μs"); // 基础的序号和时间戳列

            // 拼接所有模拟通道名称
            for (AnalogChannelDto ac : analogChannels) {
                header.append(",").append(ac.getChId());
            }
            // 拼接所有数字通道名称
            for (DigitalChannelDto dc : digitalChannels) {
                header.append(",").append(dc.getChId());
            }
            writer.write(header.toString());
            writer.newLine();

            // 2. 纯净无损：老老实实循环遍历并流式写入每一个时间维度的【所有】采样点数据，绝不擅自截断
            for (SamplePointDto point : samplePoints) {
                StringBuilder row = new StringBuilder();
                // 写入采样序号和时间戳
                row.append(point.getN()).append(",").append(point.getTimestampUs());

                // 写入当前采样点下所有模拟通道的真实还原物理量值（应用 6 位小数高精度格式化限制）
                for (Double analogVal : point.getAnalogValues()) {
                    row.append(",").append(df.format(analogVal));
                }

                // 写入当前采样点下所有数字通道的状态值 (0 或 1)
                for (Integer digitalVal : point.getDigitalValues()) {
                    row.append(",").append(digitalVal);
                }

                writer.write(row.toString());
                writer.newLine();
            }

            writer.flush();
            log.info("工业全量高精度 CSV 数据生成成功! 成功无损导出共计 [{}] 行全量波形采样记录", samplePoints.size());
        }
    }
}
