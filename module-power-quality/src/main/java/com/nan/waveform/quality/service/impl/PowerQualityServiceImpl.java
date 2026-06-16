package com.nan.waveform.quality.service.impl;

import com.nan.waveform.quality.core.calculator.ComplianceJudgeEngine;
import com.nan.waveform.quality.core.exporter.WordReportExporter;
import com.nan.waveform.quality.core.parser.ExcelParserEngine;
import com.nan.waveform.quality.domain.dto.PowerQualityDataDto;
import com.nan.waveform.quality.domain.dto.PowerQualityReportReqDto;
import com.nan.waveform.quality.domain.dto.SteadyRowDto;
import com.nan.waveform.quality.service.PowerQualityService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nan chao
 * @since 2026/6/16 10:22
 */

@Slf4j
@Service
public class PowerQualityServiceImpl implements PowerQualityService {


    @Override
    public void generateWordReport(PowerQualityReportReqDto reqDto, HttpServletResponse response) {
        try {
            // 准备传给 poi-tl 渲染的纯文本 Map
            Map<String, Object> textRenderMap = new HashMap<>();

            // 注入前端传来的台账信息
            injectFrontendData(textRenderMap, reqDto);

            WordReportExporter.exportToResponse(textRenderMap, reqDto.getFile(), reqDto.getImages(), reqDto.getTestSite(), response);

            log.info("✅ 报告导出成功！");
        } catch (Exception e) {
            log.error("❌ 报告生成失败: ", e);
            throw new RuntimeException("报告渲染引擎崩溃：" + e.getMessage(), e);
        }
    }

    private void injectFrontendData(Map<String, Object> map, PowerQualityReportReqDto dto) {
        map.put("reportNo", dto.getReportNo());
        map.put("client", dto.getClient());
        map.put("addressOfClient", dto.getAddressOfClient());
        map.put("applicant", dto.getApplicant());
        map.put("addressOfApplicant", dto.getAddressOfApplicant());
        map.put("testSite", dto.getTestSite());
        map.put("startYear", dto.getStartYear());
        map.put("startMonth", dto.getStartMonth());
        map.put("startDay", dto.getStartDay());
        map.put("endMonth", dto.getEndMonth());
        map.put("endDay", dto.getEndDay());
        map.put("temperature", dto.getTemperature());
        map.put("humidity", dto.getHumidity());
        map.put("voltageLevel", dto.getVoltageLevel());
        map.put("testReason", dto.getTestReason());
        map.put("equipmentName", dto.getEquipmentName());
        map.put("equipmentCode", dto.getEquipmentCode());
        map.put("equipmentValidDate", dto.getEquipmentValidDate());
    }

}
