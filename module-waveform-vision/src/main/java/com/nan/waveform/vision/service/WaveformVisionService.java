package com.nan.waveform.vision.service;

import com.nan.waveform.vision.domain.dto.RegionRequestDto;
import com.nan.waveform.vision.domain.vo.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/15 14:29
 */

public interface WaveformVisionService {

    /**
     * 1. 波形图像暂态最大值
     */
    List<TransientIdentifyResultVo> identifyTransientMaxValue(MultipartFile[] files, String mode);

    /**
     * 2. 波形图像稳态值
     */
    List<SteadyIdentifyResultVo> identifySteadyStateValue(MultipartFile[] files, String mode);

    /**
     * 3. 频率
     */
    List<FrequencyIdentifyResultVo> calculateFrequency(MultipartFile[] files);

    /**
     * 4. 阶跃响应时间
     */
    StepResponseResultVo calculateStepResponse(MultipartFile file, double tLeft, double tRight);

    /**
     * 5. 控制曲线响应时间
     */
    ControlCurveResultVo calculateControlCurveResponse(MultipartFile file, List<RegionRequestDto> regions);

}
