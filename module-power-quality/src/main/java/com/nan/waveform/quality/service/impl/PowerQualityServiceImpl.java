package com.nan.waveform.quality.service.impl;

import com.nan.waveform.quality.core.calculator.ComplianceJudgeEngine;
import com.nan.waveform.quality.core.exporter.WordReportExporter;
import com.nan.waveform.quality.core.parser.ExcelParserEngine;
import com.nan.waveform.quality.domain.dto.PowerQualityReportReqDto;
import com.nan.waveform.quality.service.PowerQualityService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
            // 1. 驱动防 OOM 引擎读取 Excel (提取出规整的 Map 数据集)
            Map<String, Object> coreDataMap = ExcelParserEngine.extractCoreData(reqDto.getFile());

            // 2. 驱动合规判定引擎 (根据读取到的 95% 值，生成富文本合格/标红结论)
            ComplianceJudgeEngine.injectComplianceConclusions(coreDataMap);

            // 3. 将前端表单传来的台账数据合并进入数据大盘
            injectFrontendFormData(coreDataMap, reqDto);

            // 4. 驱动 Word 模板渲染引擎，将拼装好的海量数据一键落盘导出
            WordReportExporter.exportToResponse(coreDataMap, response);

            log.info("✅ 电能质量测试数据Word报告流式输出成功！");
        } catch (Exception e) {
            log.error("❌ 报告生成失败: ", e);
            throw new RuntimeException("报告渲染引擎崩溃：" + e.getMessage(), e);
        }
    }

    /**
     * 将前端传入的台账属性，扁平化地铺入模板渲染引擎的 Map 中
     */
    private void injectFrontendFormData(Map<String, Object> map, PowerQualityReportReqDto dto) {
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

        // 工器具动态绑定
        map.put("equipmentName", dto.getEquipmentName());
        map.put("equipmentCode", dto.getEquipmentCode());
        map.put("equipmentValidDate", dto.getEquipmentValidDate());
    }
}
