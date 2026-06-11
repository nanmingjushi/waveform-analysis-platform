package com.nan.waveform.comtrade.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author nan chao
 * @since 2026/6/11 15:45
 *
 * 录波记录列表展示对象 (View Object)
 * 专门用于前端表格展示，去除了大字段，保证接口响应速度
 */


@Data
public class ComtradeRecordListVo {
    private Long id;
    private String fileName;          // 录波文件名称
    private String stationName;       // 厂站名称
    private String deviceId;          // 记录装置
    private String startTime;         // 起始时间
    private String triggerTime;       // 触发时间
    private Integer analogCount;      // 模拟通道数
    private Integer digitalCount;     // 数字通道数
    private BigDecimal lineFrequency; // 线路频率
    private Date createTime;          // 解析/上传时间
}
