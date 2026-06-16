package com.nan.waveform.quality.core.parser;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.nan.waveform.quality.domain.dto.HarmonicRowDto;
import com.nan.waveform.quality.domain.dto.PowerQualityDataDto;
import com.nan.waveform.quality.domain.dto.SteadyRowDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nan chao
 * @since 2026/6/16 10:26
 *
 * Excel解析引擎
 */

@Slf4j
public class ExcelParserEngine {

    public static PowerQualityDataDto parseAll(MultipartFile file) throws Exception {
        PowerQualityDataDto dataBox = new PowerQualityDataDto();

        try (InputStream is = file.getInputStream()) {
            ExcelReader excelReader = EasyExcel.read(is).build();

            // 1. 读取【电压谐波】Sheet：负责抓取电压谐波、电压偏差、长时间闪变
            ReadSheet volSheet = EasyExcel.readSheet("电压谐波")
                    .headRowNumber(8) // 跳过台账头
                    .registerReadListener(new VolSheetListener(dataBox))
                    .build();

            // 2. 读取【电流谐波】Sheet：负责抓取电流谐波
            ReadSheet curSheet = EasyExcel.readSheet("电流谐波")
                    .headRowNumber(8)
                    .registerReadListener(new CurSheetListener(dataBox))
                    .build();

            // 3. 读取【功率】Sheet：负责抓取频率、不平衡度
            ReadSheet powerSheet = EasyExcel.readSheet("功率")
                    .headRowNumber(8)
                    .registerReadListener(new PowerSheetListener(dataBox))
                    .build();

            excelReader.read(volSheet, curSheet, powerSheet);
            excelReader.finish();
        }
        return dataBox;
    }

    /** ================= 监听器 1：电压谐波 ================= */
    private static class VolSheetListener extends AnalysisEventListener<Map<Integer, String>> {
        private final PowerQualityDataDto dataBox;
        public VolSheetListener(PowerQualityDataDto dataBox) { this.dataBox = dataBox; }

        @Override
        public void invoke(Map<Integer, String> row, AnalysisContext ctx) {
            String param = row.getOrDefault(0, "").trim();
            if (param.isEmpty()) return;

            // 抓取基波和2-25次谐波
            if (param.equals("基波电压(V)") || (param.matches("^\\d+$") && Integer.parseInt(param) <= 25)) {
                dataBox.getVolHarmonics().add(extractHarmonic(param, row));
            }
            // 抓取 THD
            else if (param.contains("电压总畸变率")) {
                dataBox.getVolHarmonics().add(extractHarmonic("THD", row));
            }
            // 抓取 电压偏差
            else if (param.contains("电压偏差")) {
                dataBox.setVoltageDeviation(extractSteadyMax(param, row));
            }
            // 抓取 长时间闪变
            else if (param.contains("长时间闪变")) {
                dataBox.setFlicker(extractSteadyMax(param, row));
            }
        }
        @Override public void doAfterAllAnalysed(AnalysisContext ctx) {}
    }

    /** ================= 监听器 2：电流谐波 ================= */
    private static class CurSheetListener extends AnalysisEventListener<Map<Integer, String>> {
        private final PowerQualityDataDto dataBox;
        public CurSheetListener(PowerQualityDataDto dataBox) { this.dataBox = dataBox; }

        @Override
        public void invoke(Map<Integer, String> row, AnalysisContext ctx) {
            String param = row.getOrDefault(0, "").trim();
            if (param.isEmpty()) return;

            // 抓取基波和2-25次谐波
            if (param.equals("基波电流(A)") || (param.matches("^\\d+$") && Integer.parseInt(param) <= 25)) {
                dataBox.getCurHarmonics().add(extractHarmonic(param, row));
            }
        }
        @Override public void doAfterAllAnalysed(AnalysisContext ctx) {}
    }

    /** ================= 监听器 3：功率/稳态 ================= */
    private static class PowerSheetListener extends AnalysisEventListener<Map<Integer, String>> {
        private final PowerQualityDataDto dataBox;
        public PowerSheetListener(PowerQualityDataDto dataBox) { this.dataBox = dataBox; }

        @Override
        public void invoke(Map<Integer, String> row, AnalysisContext ctx) {
            String param = row.getOrDefault(0, "").trim();

            if (param.contains("频率")) {
                dataBox.setFrequency(extractSteadyMax(param, row));
            } else if (param.contains("不平衡度")) {
                dataBox.setUnbalance(extractSteadyMax(param, row));
            }
        }
        @Override public void doAfterAllAnalysed(AnalysisContext ctx) {}
    }

    /** --- 内部提取工具：谐波行提取 --- */
    private static HarmonicRowDto extractHarmonic(String order, Map<Integer, String> row) {
        HarmonicRowDto dto = new HarmonicRowDto();
        dto.setOrder(order);
        // 按实际 CSV 索引：Avg 是 3, 8, 13；95% 是 5, 10, 15；限值是 17
        dto.setAbAvg(format(row.get(3)));
        dto.setAb95(format(row.get(5)));
        dto.setBcAvg(format(row.get(8)));
        dto.setBc95(format(row.get(10)));
        dto.setCaAvg(format(row.get(13)));
        dto.setCa95(format(row.get(15)));
        dto.setLimitVal(format(row.get(17)));
        return dto;
    }

    /** --- 内部提取工具：稳态综合行提取 (取三相中最恶劣/最大值) --- */
    private static SteadyRowDto extractSteadyMax(String name, Map<Integer, String> row) {
        SteadyRowDto dto = new SteadyRowDto();
        dto.setItemName(name);
        // Max(2,7,12), Avg(3,8,13), Min(4,9,14), 95%(5,10,15)
        dto.setMaxVal(maxAmongPhases(row.get(2), row.get(7), row.get(12)));
        dto.setAvgVal(maxAmongPhases(row.get(3), row.get(8), row.get(13)));
        dto.setMinVal(maxAmongPhases(row.get(4), row.get(9), row.get(14)));
        dto.setVal95(maxAmongPhases(row.get(5), row.get(10), row.get(15)));
        dto.setLimitVal(format(row.get(17)));
        return dto;
    }

    private static String maxAmongPhases(String a, String b, String c) {
        double valA = parse(a); double valB = parse(b); double valC = parse(c);
        double max = Math.max(valA, Math.max(valB, valC));
        return max == -9999.0 ? "--" : String.format("%.2f", max);
    }

    private static double parse(String val) {
        try { return (val == null || val.trim().isEmpty() || val.contains("--")) ? -9999.0 : Double.parseDouble(val.trim()); }
        catch (Exception e) { return -9999.0; }
    }

    private static String format(String val) {
        if (val == null || val.trim().isEmpty()) return "--";
        try { return String.format("%.2f", Double.parseDouble(val.trim())); }
        catch (Exception e) { return val.trim(); }
    }
}

