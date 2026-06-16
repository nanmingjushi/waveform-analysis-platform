package com.nan.waveform.quality.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author nan chao
 * @since 2026/6/16 10:24
 *
 * 谐波的映射行 DTO (用于装载从 Excel 读出的单行数据，送入 Word 引擎)
 */

@Data
public class HarmonicRowDto {

    private String order;      // 谐波次序 (基波, 2, 3... THD)

    private String abAvg;      // AB相 平均值 (占位或实际值)
    private String ab95;       // AB相 95% 值

    private String bcAvg;      // BC相 平均值
    private String bc95;       // BC相 95% 值

    private String caAvg;      // CA相 平均值
    private String ca95;       // CA相 95% 值

    private String limitVal;   // 限值
}
