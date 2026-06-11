package com.nan.waveform.comtrade.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author nan chao
 * @since 2026/6/11 15:42
 *
 * COMTRADE 录波解析记录实体类 (对应 comtrade_record 表)
 */

@Data
public class ComtradeRecord {
    private Long id;
    private Long userId; // 上传者ID
    private String fileName;        // 录波文件名称
    private String stationName;     // 厂站名称
    private String deviceId;        // 记录装置
    private String startTime;       // 起始时间
    private String triggerTime;     // 触发时间
    private Integer analogCount;    // 模拟通道数
    private Integer digitalCount;   // 数字通道数
    private BigDecimal lineFrequency; // 名义线路频率
    private String cfgFilePath;     // cfg物理文件保存路径
    private String datFilePath;     // dat物理文件保存路径
    private Date createTime;        // 创建/解析时间
}
