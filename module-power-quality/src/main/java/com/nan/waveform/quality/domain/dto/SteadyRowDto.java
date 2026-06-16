package com.nan.waveform.quality.domain.dto;

import lombok.Data;

/**
 * @author nan chao
 * @since 2026/6/16 11:26
 *
 */

@Data
public class SteadyRowDto {
    private String maxVal;
    private String avgVal;
    private String minVal;
    private String val95;
    private String limitVal;
}
