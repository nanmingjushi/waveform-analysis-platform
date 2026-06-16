package com.nan.waveform.quality.core.parser;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.nan.waveform.quality.domain.dto.DeviationRowDto;
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
 * <p>
 * Excel解析引擎
 */

@Slf4j
public class ExcelParserEngine {
    public static PowerQualityDataDto parseAll(MultipartFile file) throws Exception {
        PowerQualityDataDto dataBox = new PowerQualityDataDto();

        try (InputStream is1 = file.getInputStream();
             InputStream is2 = file.getInputStream();
             InputStream is3 = file.getInputStream()) {

            // 同步一次性捞出所有 Sheet 的绝对物理行
            List<Map<Integer, String>> volRows = EasyExcel.read(is1).sheet("电压谐波").doReadSync();
            List<Map<Integer, String>> curRows = EasyExcel.read(is2).sheet("电流谐波").doReadSync();
            List<Map<Integer, String>> powerRows = EasyExcel.read(is3).sheet("功率").doReadSync();

            // 1. 严格抓取电压谐波 (基波=9, 2-25次=10~33, THD=59)
            dataBox.getVolHarmonics().add(extractHarmonicRow(volRows, 9));
            for (int i = 10; i <= 33; i++) {
                dataBox.getVolHarmonics().add(extractHarmonicRow(volRows, i));
            }
            dataBox.getVolHarmonics().add(extractHarmonicRow(volRows, 59));

            // 2. 严格抓取电流谐波 (基波=9, 2-25次=10~33)
            dataBox.getCurHarmonics().add(extractHarmonicRow(curRows, 9));
            for (int i = 10; i <= 33; i++) {
                dataBox.getCurHarmonics().add(extractHarmonicRow(curRows, i));
            }

            // 3. 抓取表1.3需要的稳态行 (频率=功率表15, 不平衡度=功率表16)
            dataBox.setFrequency(extractSteadyRow(powerRows, 15));
            dataBox.setUnbalance(extractSteadyRow(powerRows, 16));
            // 闪变 = 电压表61行，拆出3个通道
            dataBox.setFlickerAB(extractFlickerPhase(volRows, 61, 2, 3, 4, 5, 17));
            dataBox.setFlickerBC(extractFlickerPhase(volRows, 61, 7, 8, 9, 10, 17));
            dataBox.setFlickerAC(extractFlickerPhase(volRows, 61, 12, 13, 14, 15, 17));

            // 4. 抓取表1.4电压偏差 (上偏差=电压表63, 下偏差=电压表64)
            dataBox.setDeviationUp(extractDeviationRow(volRows, 63));
            dataBox.setDeviationDown(extractDeviationRow(volRows, 64));
        }
        return dataBox;
    }

    private static HarmonicRowDto extractHarmonicRow(List<Map<Integer, String>> rows, int index) {
        HarmonicRowDto dto = new HarmonicRowDto();
        if (index >= rows.size()) return dto;
        Map<Integer, String> row = rows.get(index);
        dto.setAbAvg(row.get(3));  dto.setAb95(row.get(5));
        dto.setBcAvg(row.get(8));  dto.setBc95(row.get(10));
        dto.setCaAvg(row.get(13)); dto.setCa95(row.get(15));
        dto.setLimitVal(row.get(17));
        return dto;
    }

    private static SteadyRowDto extractSteadyRow(List<Map<Integer, String>> rows, int index) {
        SteadyRowDto dto = new SteadyRowDto();
        if (index >= rows.size()) return dto;
        Map<Integer, String> row = rows.get(index);
        dto.setMaxVal(row.get(2));  dto.setAvgVal(row.get(3));
        dto.setMinVal(row.get(4));  dto.setVal95(row.get(5));
        dto.setLimitVal(row.get(17));
        return dto;
    }

    private static SteadyRowDto extractFlickerPhase(List<Map<Integer, String>> rows, int index, int max, int avg, int min, int v95, int lim) {
        SteadyRowDto dto = new SteadyRowDto();
        if (index >= rows.size()) return dto;
        Map<Integer, String> row = rows.get(index);
        dto.setMaxVal(row.get(max));  dto.setAvgVal(row.get(avg));
        dto.setMinVal(row.get(min));  dto.setVal95(row.get(v95));
        dto.setLimitVal(row.get(lim));
        return dto;
    }

    private static DeviationRowDto extractDeviationRow(List<Map<Integer, String>> rows, int index) {
        DeviationRowDto dto = new DeviationRowDto();
        if (index >= rows.size()) return dto;
        Map<Integer, String> row = rows.get(index);
        dto.setAbMax(row.get(2));   dto.setAbMin(row.get(4));
        dto.setBcMax(row.get(7));   dto.setBcMin(row.get(9));
        dto.setAcMax(row.get(12));  dto.setAcMin(row.get(14));
        dto.setLimitVal(row.get(17));
        return dto;
    }

}

