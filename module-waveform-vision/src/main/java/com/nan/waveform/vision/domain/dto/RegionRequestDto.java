package com.nan.waveform.vision.domain.dto;

import lombok.Data;

/**
 * @author nan chao
 * @since 2026/6/15 11:28
 *
 * 控制曲线响应时间子业务：因为前端要图像选框，前端给后端会传入参数请求实体
 * 其他四个子业务不需要dto
 */

@Data
public class RegionRequestDto {
    private int x;              // 选区物理起点 X 坐标
    private int y;              // 选区物理起点 Y 坐标
    private int w;              // 选区物理宽度
    private int h;              // 选区物理高度
    private double tLeftSec;    // 该选区左边界对应的物理时间 (s)
    private double tRightSec;   // 该选区右边界对应的物理时间 (s)
}
