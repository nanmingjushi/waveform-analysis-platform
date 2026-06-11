package com.nan.waveform.comtrade.domain.dto;

import lombok.Data;

/**
 * @author nan chao
 * @since 2026/6/11 15:44
 *
 * 数字通道数据模型
 */

@Data
public class DigitalChannelDto {
    private String dn;      // 通道序号
    private String chId;    // 通道名称
    private String ph;      // 相位
    private String ccbm;    // 被监测元件
    private String y;       // 正常状态 (0或1)
}
