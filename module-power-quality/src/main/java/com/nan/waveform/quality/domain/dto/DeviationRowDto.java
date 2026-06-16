package com.nan.waveform.quality.domain.dto;

import lombok.Data;

/**
 * @author nan chao
 * @since 2026/6/16 16:09
 *
 * 电压偏差
 */
@Data
public class DeviationRowDto {
    private String abMax;
    private String abMin;
    private String bcMax;
    private String bcMin;
    private String acMax;
    private String acMin;
    private String limitVal;
}
