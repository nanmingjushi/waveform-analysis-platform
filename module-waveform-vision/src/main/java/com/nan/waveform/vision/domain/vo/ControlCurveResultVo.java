package com.nan.waveform.vision.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/15 11:21
 *
 * 控制曲线响应时间
 */

@Data
public class ControlCurveResultVo {
    private String fileName;               // 原始图像文件名
    private List<RegionResultVo> results;  // 前端自定义框选的 N 个选区识别矩阵

    @Data
    public static class RegionResultVo {
        private Integer regionIndex;       // 选区序号 (1, 2, 3...N)
        private Integer x;                 // 原始物理选区起点 X 坐标
        private Integer y;                 // 原始物理选区起点 Y 坐标
        private Integer w;                 // 选区物理宽度
        private Integer h;                 // 选区物理高度
        private Double timeBlue;           // 捕获到的蓝色控制线物理时间 (s)
        private Double timeGreen;          // 捕获到的绿色响应线物理时间 (s)
        private Double responseTime;       // 两者时延差：timeBlue - timeGreen (s)
        private String note;               // 智能辅助诊断备注
    }
}
