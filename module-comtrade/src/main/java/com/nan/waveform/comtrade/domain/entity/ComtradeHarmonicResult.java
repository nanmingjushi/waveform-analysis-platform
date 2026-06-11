package com.nan.waveform.comtrade.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author nan chao
 * @since 2026/6/11 15:43
 *
 * 通道谐波分析结果实体类 (对应 comtrade_harmonic_result 表)
 */

@Data
public class ComtradeHarmonicResult {
    private Long id;
    private Long recordId;          // 关联的录波记录ID
    private Integer channelIndex;   // 通道序号
    private String channelIdName;   // 通道标识/名称 (如 VA, IB)
    private String channelType;     // 通道类型: U(电压) 或 I(电流)
    private BigDecimal sampleFrequency; // 采样频率
    private BigDecimal thdValue;    // 总谐波畸变率 THD (%)

    // 存入数据库时，我们会把 List 转换成 JSON 字符串存入这个字段
    private String harmonicRatioJson; // 各次谐波含有率JSON
    private Date createTime;
}
