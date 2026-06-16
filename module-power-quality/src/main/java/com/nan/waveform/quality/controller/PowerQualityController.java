package com.nan.waveform.quality.controller;

import com.nan.waveform.quality.domain.dto.PowerQualityReportReqDto;
import com.nan.waveform.quality.service.PowerQualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * 接收前端填写的表单与 Excel，直接响应 Word 文件流下载
     */
    @Operation(summary = "解析Excel并导出Word报告", description = "前端以FormData形式提交台账参数与Excel文件，系统返回docx文件流")
    @PostMapping(value = "/generate-report", consumes = "multipart/form-data")
    public void generateReport(@ModelAttribute PowerQualityReportReqDto reqDto, HttpServletResponse response) {
        log.info("🚀 接收到电能质量测试数据报告生成请求，委托单位: {}, 核心文件: {}",
                reqDto.getClient(),
                reqDto.getFile() != null ? reqDto.getFile().getOriginalFilename() : "未上传");

        // 文件空壳校验与异常阻断
        if (reqDto.getFile() == null || reqDto.getFile().isEmpty()) {
            throw new IllegalArgumentException("严重拦截：电能质量测试数据源文件 (Excel) 不能为空！");
        }

        String fileName = reqDto.getFile().getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx"))) {
            throw new IllegalArgumentException("格式阻断：上传的文件必须是合法的 Excel 格式 (.xls 或 .xlsx)！");
        }

        // 调用业务引擎
        powerQualityService.generateWordReport(reqDto, response);
    }
}
