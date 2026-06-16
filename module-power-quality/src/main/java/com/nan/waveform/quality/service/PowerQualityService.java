package com.nan.waveform.quality.service;

import com.nan.waveform.quality.domain.dto.PowerQualityReportReqDto;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author nan chao
 * @since 2026/6/16 10:21
 */

public interface PowerQualityService {
    /**
     * 解析 Excel 数据并生成 Word 审计报告
     */
    void generateWordReport(PowerQualityReportReqDto reqDto, HttpServletResponse response);
}
