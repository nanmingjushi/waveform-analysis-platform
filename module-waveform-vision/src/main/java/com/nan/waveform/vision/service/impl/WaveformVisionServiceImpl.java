package com.nan.waveform.vision.service.impl;

import com.nan.waveform.vision.config.WaveformVisionProperties;
import com.nan.waveform.vision.domain.dto.RegionRequestDto;
import com.nan.waveform.vision.domain.vo.*;
import com.nan.waveform.vision.engine.OpenCvVisionEngine;
import com.nan.waveform.vision.service.WaveformVisionService;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author nan chao
 * @since 2026/6/15 14:30
 */

@Slf4j
@Service
public class WaveformVisionServiceImpl implements WaveformVisionService {
    @Autowired
    private WaveformVisionProperties properties;

    @Override
    public List<TransientIdentifyResultVo> identifyTransientMaxValue(MultipartFile[] files, String mode) {
        final double perSegmentValue = "current".equalsIgnoreCase(mode) ? 500.0 : 200000.0;
        List<TransientIdentifyResultVo> outList = new ArrayList<>();

        for (MultipartFile file : files) {
            TransientIdentifyResultVo fileVo = new TransientIdentifyResultVo();
            fileVo.setFileName(file.getOriginalFilename());
            List<TransientIdentifyResultVo.TransientPhaseVo> phaseVoList = new ArrayList<>();

            Mat img = null;
            try {
                img = multipartFileToMat(file);
                if (img.empty()) throw new IllegalArgumentException("无法解码图片矩阵");

                // 从 yml 配置中拉取裁剪框，完美解耦
                String[] phaseNames = {"A", "B", "C"};
                Rect[] rois = {
                        toOpenCvRect(properties.getRoi().getPhaseA()),
                        toOpenCvRect(properties.getRoi().getPhaseB()),
                        toOpenCvRect(properties.getRoi().getPhaseC())
                };

                for (int i = 0; i < 3; i++) {
                    TransientIdentifyResultVo.TransientPhaseVo pVo = new TransientIdentifyResultVo.TransientPhaseVo();
                    pVo.setPhase(phaseNames[i]);

                    Mat part = null;
                    try {
                        part = new Mat(img, rois[i]);
                        Map<String, Object> r = OpenCvVisionEngine.computeTransientMaxValue(part, perSegmentValue);

                        if (r.containsKey("error")) {
                            pVo.setError((String) r.get("error"));
                        } else {
                            pVo.setValue((Double) r.get("value"));
                            pVo.setWaveTopY((Integer) r.get("waveTopY"));
                        }
                    } catch (Exception e) {
                        pVo.setError("切相算力崩溃: " + e.getMessage());
                    } finally {
                        if (part != null) part.release(); // 显式释放子矩阵
                    }
                    phaseVoList.add(pVo);
                }
            } catch (Exception e) {
                log.error("暂态最大值文件级解析异常: ", e);
                // 文件级别熔断容错处理
                TransientIdentifyResultVo.TransientPhaseVo errPhase = new TransientIdentifyResultVo.TransientPhaseVo();
                errPhase.setPhase("ALL");
                errPhase.setError("核心死机: " + e.getMessage());
                phaseVoList.add(errPhase);
            } finally {
                if (img != null) img.release(); // 释放大图 C++ 本地内存
            }

            fileVo.setPhases(phaseVoList);
            outList.add(fileVo);
        }
        return outList;
    }

    @Override
    public List<SteadyIdentifyResultVo> identifySteadyStateValue(MultipartFile[] files, String mode) {
        final boolean isVoltage = !"current".equalsIgnoreCase(mode);
        final double perSegmentValue = isVoltage ? 200000.0 : 500.0;
        final double displayScale = isVoltage ? (1.0 / 1000.0) : 1.0; // 电压转kV，电流保持A

        List<SteadyIdentifyResultVo> outList = new ArrayList<>();

        for (MultipartFile file : files) {
            SteadyIdentifyResultVo fileVo = new SteadyIdentifyResultVo();
            fileVo.setFileName(file.getOriginalFilename());
            fileVo.setMode(mode);
            fileVo.setUnit(isVoltage ? "V" : "A");
            List<SteadyIdentifyResultVo.SteadyPhaseVo> phaseVoList = new ArrayList<>();

            Mat img = null;
            try {
                img = multipartFileToMat(file);
                if (img.empty()) throw new IllegalArgumentException("图片空壳，拒绝入舱");

                String[] phaseNames = {"A", "B", "C"};
                Rect[] rois = {
                        toOpenCvRect(properties.getRoi().getPhaseA()),
                        toOpenCvRect(properties.getRoi().getPhaseB()),
                        toOpenCvRect(properties.getRoi().getPhaseC())
                };

                for (int i = 0; i < 3; i++) {
                    SteadyIdentifyResultVo.SteadyPhaseVo pVo = new SteadyIdentifyResultVo.SteadyPhaseVo();
                    pVo.setPhase(phaseNames[i]);

                    Mat part = null;
                    try {
                        part = new Mat(img, rois[i]);
                        Map<String, Object> r = OpenCvVisionEngine.computeSteadyStateValue(part, perSegmentValue, displayScale);

                        if (r.containsKey("error")) {
                            pVo.setError((String) r.get("error"));
                        } else {
                            pVo.setSteadyPeakV((Double) r.get("steadyPeakV"));
                            pVo.setSteadyRmsV((Double) r.get("steadyRmsV"));
                            pVo.setSampleRmsV((Double) r.get("sampleRmsV"));
                        }
                    } catch (Exception e) {
                        pVo.setError("稳态算法崩溃: " + e.getMessage());
                    } finally {
                        if (part != null) part.release(); // 释放内存
                    }
                    phaseVoList.add(pVo);
                }
            } catch (Exception e) {
                log.error("稳态值文件解析失败: ", e);
                SteadyIdentifyResultVo.SteadyPhaseVo err = new SteadyIdentifyResultVo.SteadyPhaseVo();
                err.setPhase("ALL");
                err.setError(e.getMessage());
                phaseVoList.add(err);
            } finally {
                if (img != null) img.release(); // 显式释放
            }

            fileVo.setPhases(phaseVoList);
            outList.add(fileVo);
        }
        return outList;
    }

    @Override
    public List<FrequencyIdentifyResultVo> calculateFrequency(MultipartFile[] files) {
        List<FrequencyIdentifyResultVo> outList = new ArrayList<>();

        for (MultipartFile file : files) {
            FrequencyIdentifyResultVo fileVo = new FrequencyIdentifyResultVo();
            fileVo.setFileName(file.getOriginalFilename());
            List<FrequencyIdentifyResultVo.FrequencyPhaseVo> phaseVoList = new ArrayList<>();

            Mat img = null;
            try {
                img = multipartFileToMat(file);
                if (img.empty()) throw new IllegalArgumentException("无法读取有效像素阵列");

                String[] phaseNames = {"A", "B", "C"};
                Rect[] rois = {
                        toOpenCvRect(properties.getRoi().getPhaseA()),
                        toOpenCvRect(properties.getRoi().getPhaseB()),
                        toOpenCvRect(properties.getRoi().getPhaseC())
                };

                for (int i = 0; i < 3; i++) {
                    FrequencyIdentifyResultVo.FrequencyPhaseVo pVo = new FrequencyIdentifyResultVo.FrequencyPhaseVo();
                    pVo.setPhase(phaseNames[i]);

                    Mat part = null;
                    try {
                        part = new Mat(img, rois[i]);
                        Map<String, Object> r = OpenCvVisionEngine.computeFrequency(part);

                        if (r.containsKey("error")) {
                            pVo.setError((String) r.get("error"));
                        } else {
                            pVo.setFreqHz((Double) r.get("freqHz"));
                            pVo.setPeriodMs((Double) r.get("periodMs"));
                        }
                    } catch (Exception e) {
                        pVo.setError("自相关测算崩溃: " + e.getMessage());
                    } finally {
                        if (part != null) part.release(); // 显式释放
                    }
                    phaseVoList.add(pVo);
                }
            } catch (Exception e) {
                log.error("频率分析失败: ", e);
                FrequencyIdentifyResultVo.FrequencyPhaseVo err = new FrequencyIdentifyResultVo.FrequencyPhaseVo();
                err.setPhase("ALL");
                err.setError(e.getMessage());
                phaseVoList.add(err);
            } finally {
                if (img != null) img.release(); // 防火墙释放
            }

            fileVo.setPhases(phaseVoList);
            outList.add(fileVo);
        }
        return outList;
    }

    @Override
    public StepResponseResultVo calculateStepResponse(MultipartFile file, double tLeft, double tRight) {
        StepResponseResultVo vo = new StepResponseResultVo();
        vo.setFileName(file.getOriginalFilename());

        Mat img = null;
        Mat cropped = null;
        try {
            img = multipartFileToMat(file);
            if (img.empty()) throw new IllegalArgumentException("阶跃原图无法解码");

            // 从 yml 安全拉取阶跃 ROI 固化裁剪区
            Rect roiRect = toOpenCvRect(properties.getRoi().getJieyue());
            cropped = new Mat(img, roiRect);

            Map<String, Object> r = OpenCvVisionEngine.computeStepResponse(cropped, tLeft, tRight);
            vo.setT5((Double) r.get("t5"));
            vo.setT95((Double) r.get("t95"));
            vo.setTStep((Double) r.get("tStep"));

        } catch (Exception e) {
            log.error("阶跃时间转换熔断: ", e);
            vo.setError("转换隔离: " + e.getMessage());
        } finally {
            if (cropped != null) cropped.release();
            if (img != null) img.release(); // 彻底擦除物理内存
        }
        return vo;
    }

    @Override
    public ControlCurveResultVo calculateControlCurveResponse(MultipartFile file, List<RegionRequestDto> regions) {
        ControlCurveResultVo wrapperVo = new ControlCurveResultVo();
        wrapperVo.setFileName(file.getOriginalFilename());
        List<ControlCurveResultVo.RegionResultVo> voResults = new ArrayList<>();

        Mat img = null;
        try {
            img = multipartFileToMat(file);
            if (img.empty()) throw new IllegalArgumentException("控制响应曲线原图空壳");

            int index = 1;
            for (RegionRequestDto dto : regions) {
                ControlCurveResultVo.RegionResultVo rVo = new ControlCurveResultVo.RegionResultVo();
                rVo.setRegionIndex(index++);
                rVo.setX(dto.getX());
                rVo.setY(dto.getY());
                rVo.setW(dto.getW());
                rVo.setH(dto.getH());

                // 动态根据前端前端拖拽框选的像素范围，进行安全的亚像素边界矩形防御生成
                int safeX = Math.max(0, Math.min(dto.getX(), img.width() - 1));
                int safeY = Math.max(0, Math.min(dto.getY(), img.height() - 1));
                int safeW = Math.max(1, Math.min(dto.getW(), img.width() - safeX));
                int safeH = Math.max(1, Math.min(dto.getH(), img.height() - safeY));

                Rect customRect = new Rect(safeX, safeY, safeW, safeH);
                Mat roiBGR = null;
                try {
                    roiBGR = new Mat(img, customRect);
                    Map<String, Object> r = OpenCvVisionEngine.computeControlCurveResponse(roiBGR, dto.getTLeftSec(), dto.getTRightSec());

                    if (r.containsKey("note")) {
                        rVo.setNote((String) r.get("note"));
                    } else {
                        rVo.setTimeBlue((Double) r.get("timeBlue"));
                        rVo.setTimeGreen((Double) r.get("timeGreen"));
                        rVo.setResponseTime((Double) r.get("responseTime"));
                    }
                } catch (Exception e) {
                    rVo.setNote("区域算力溃败: " + e.getMessage());
                } finally {
                    if (roiBGR != null) roiBGR.release(); // 局部选区即时释放
                }
                voResults.add(rVo);
            }
        } catch (Exception e) {
            log.error("控制曲线多区域计算大面积熔断: ", e);
            ControlCurveResultVo.RegionResultVo errVo = new ControlCurveResultVo.RegionResultVo();
            errVo.setRegionIndex(0);
            errVo.setNote("全局崩溃: " + e.getMessage());
            voResults.add(errVo);
        } finally {
            if (img != null) img.release();
        }

        wrapperVo.setResults(voResults);
        return wrapperVo;
    }


    private Mat multipartFileToMat(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        return Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_COLOR);
    }

    private Rect toOpenCvRect(WaveformVisionProperties.Region region) {
        return new Rect(region.getX(), region.getY(), region.getW(), region.getH());
    }
}
