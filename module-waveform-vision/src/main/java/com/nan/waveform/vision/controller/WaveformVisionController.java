package com.nan.waveform.vision.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nan.waveform.common.result.Result;
import com.nan.waveform.vision.domain.dto.RegionRequestDto;
import com.nan.waveform.vision.domain.vo.*;
import com.nan.waveform.vision.service.WaveformVisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/15 14:42
 */

@Slf4j
@RestController
@RequestMapping("/api/waveform-vision")
@Tag(name = "波形图像的识别与关键参数提取", description = "波形图像暂态最大值识别，波形图像稳态值识别，频率计算，阶跃响应时间，控制曲线响应时间")
public class WaveformVisionController {

    @Autowired
    private WaveformVisionService waveformVisionService;

    @Autowired
    private ObjectMapper objectMapper; // 引入 Spring 默认的 Jackson 解析器，用于防御性解析多段混合流

    /**
     * 1. 波形图像暂态最大值识别
     */
    @Operation(summary = "1. 波形图像暂态最大值", description = "波形图像暂态最大值")
    @PostMapping(value = "/transient/max-value", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<List<TransientIdentifyResultVo>> identifyTransientMaxValue(
            @Parameter(description = "待解析的波形图像文件数组", required = true) @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "物理分类标识：voltage(电压) / current(电流)", required = true) @RequestParam("mode") String mode) {

        log.info("🚀接收到暂态最大值识别请求, 文件数: {}, 模式: {}", files.length, mode);
        List<TransientIdentifyResultVo> results = waveformVisionService.identifyTransientMaxValue(files, mode);
        return Result.success(results);
    }

    /**
     * 2. 波形图像稳态值识别
     */
    @Operation(summary = "2. 波形图像稳态值识别", description = "波形图像稳态值识别")
    @PostMapping(value = "/steady-state/value", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<List<SteadyIdentifyResultVo>> identifySteadyStateValue(
            @Parameter(description = "待解析的波形图像文件数组", required = true) @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "物理分类标识：voltage(电压) / current(电流)", required = true) @RequestParam("mode") String mode) {

        log.info("🚀接收到稳态值识别请求, 文件数: {}, 模式: {}", files.length, mode);
        List<SteadyIdentifyResultVo> results = waveformVisionService.identifySteadyStateValue(files, mode);
        return Result.success(results);
    }

    /**
     * 3. 频率计算
     */
    @Operation(summary = "3. 频率计算", description = "频率计算")
    @PostMapping(value = "/frequency", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<List<FrequencyIdentifyResultVo>> calculateFrequency(
            @Parameter(description = "待解析的波形图像文件数组", required = true) @RequestParam("files") MultipartFile[] files) {

        log.info("🚀接收到频率计算请求, 文件数: {}", files.length);
        List<FrequencyIdentifyResultVo> results = waveformVisionService.calculateFrequency(files);
        return Result.success(results);
    }

    /**
     * 4. 阶跃响应时间
     */
    @Operation(summary = "4. 阶跃响应时间", description = "阶跃响应时间")
    @PostMapping(value = "/step-response", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<StepResponseResultVo> calculateStepResponse(
            @Parameter(description = "单张阶跃响应波形图", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "图像左边界代表的物理时间(s)", required = true) @RequestParam("tLeft") double tLeft,
            @Parameter(description = "图像右边界代表的物理时间(s)", required = true) @RequestParam("tRight") double tRight) {

        log.info("🚀接收到阶跃响应时间识别请求, 文件名: {}, 时间轴: [{}s ~ {}s]", file.getOriginalFilename(), tLeft, tRight);
        StepResponseResultVo result = waveformVisionService.calculateStepResponse(file, tLeft, tRight);
        return Result.success(result);
    }

    /**
     * 5. 控制曲线响应时间
     */
    @Operation(summary = "5. 控制曲线响应时间", description = "控制曲线响应时间")
    @PostMapping(value = "/control-curve/response-time", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ControlCurveResultVo> calculateControlCurveResponse(
            @Parameter(description = "包含控制线(蓝)与响应线(绿)的混合波形图", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "前端多选区参数明细 (JSON 字符串)", required = true) @RequestParam("regions") String regionsJson) {

        log.info("🚀接收到控制曲线响应时间请求, 文件名: {}", file.getOriginalFilename());
        try {
            List<RegionRequestDto> regions = objectMapper.readValue(regionsJson, new TypeReference<List<RegionRequestDto>>() {
            });
            ControlCurveResultVo result = waveformVisionService.calculateControlCurveResponse(file, regions);
            return Result.success(result);
        } catch (Exception e) {
            log.error("💥选区参数序列化拦截熔断: ", e);
            ControlCurveResultVo errWrapper = new ControlCurveResultVo();
            errWrapper.setFileName(file.getOriginalFilename());
            return Result.success(errWrapper);
        }
    }
}