package com.nan.waveform.vision.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author nan chao
 * @since 2026/6/15 11:09
 *
 * 将 module-admin 中 application.yml 的 waveform-vision 参数映射到 Java 属性对象中
 */

@Data
@Component
@ConfigurationProperties(prefix = "waveform-vision")
public class WaveformVisionProperties {
    private RoiProperties roi;
    private HsvProperties hsv;

    @Data
    public static class RoiProperties {
        private Region phaseA;
        private Region phaseB;
        private Region phaseC;
        private Region jieyue;
    }

    @Data
    public static class Region {
        private int x;
        private int y;
        private int w;
        private int h;
    }

    @Data
    public static class HsvProperties {
        private ColorRange green;
        private ColorRange blue;
    }

    @Data
    public static class ColorRange {
        private double[] low;
        private double[] high;
    }
}
