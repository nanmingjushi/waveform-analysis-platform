package com.nan.waveform.quality.controller;

import com.nan.waveform.quality.domain.dto.PowerQualityReportReqDto;
import com.nan.waveform.quality.service.PowerQualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author nan chao
 * @since 2026/6/16 10:21
 */


@Slf4j
@RestController
@RequestMapping("/api/power-quality")
@Tag(name = "电能质量测试数据报告自动化生成", description = "电能质量测试数据报告自动化生成")
public class PowerQualityController {

    @Autowired
    private PowerQualityService powerQualityService;

    @Operation(summary = "解析Excel并直接导出Word报告")
    @PostMapping(value = "/generate-report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void generateReport(@ModelAttribute PowerQualityReportReqDto reqDto, HttpServletResponse response) {
        if (reqDto.getFile() == null || reqDto.getFile().isEmpty()) {
            throw new IllegalArgumentException("Excel 数据源文件不能为空！");
        }
        powerQualityService.generateWordReport(reqDto, response);
    }
}
