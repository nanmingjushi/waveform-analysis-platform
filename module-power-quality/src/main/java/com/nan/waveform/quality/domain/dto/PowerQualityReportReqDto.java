package com.nan.waveform.quality.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author nan chao
 * @since 2026/6/16 10:23
 *
 * 电能质量报告生成请求体（包含现场台账表单 + Excel源文件）
 * 前端网页表单信息填好后，连同Excel一起提交过来的对象
 */

@Data
@Schema(description = "电能质量测试数据报告生成请求对象")
public class PowerQualityReportReqDto {

    @Schema(description = "报告编号", example = "ABCDEF202501011")
    private String reportNo;

    @Schema(description = "委托单位", example = "**********有限公司")
    private String client;

    @Schema(description = "委托单位地址", example = "**市**区******")
    private String addressOfClient;

    @Schema(description = "被测单位", example = "**市******股份有限公司")
    private String applicant;

    @Schema(description = "被测单位地址", example = "**市**区")
    private String addressOfApplicant;

    @Schema(description = "测试地点", example = "**市************")
    private String testSite;

    @Schema(description = "测试开始-年", example = "2025")
    private String startYear;

    @Schema(description = "测试开始-月", example = "01")
    private String startMonth;

    @Schema(description = "测试开始-日", example = "01")
    private String startDay;

    @Schema(description = "测试结束-月", example = "01")
    private String endMonth;

    @Schema(description = "测试结束-日", example = "30")
    private String endDay;

    @Schema(description = "环境温度(℃)", example = "15")
    private String temperature;

    @Schema(description = "相对湿度(%)", example = "60")
    private String humidity;

    @Schema(description = "测试电压等级", example = "10kV")
    private String voltageLevel;

    @Schema(description = "测试原因", example = "电能质量测试")
    private String testReason;

    @Schema(description = "测试设备名称", example = "电能质量分析仪 FLUKE-1777")
    private String equipmentName;

    @Schema(description = "测试设备证书编号", example = "J202309183499-04-0001")
    private String equipmentCode;

    @Schema(description = "测试设备有效期", example = "2025.6.23")
    private String equipmentValidDate;

    @Schema(description = "源数据Excel文件", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile file;
}
