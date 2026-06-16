package com.nan.waveform.quality.core.exporter;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.deepoove.poi.XWPFTemplate;
import com.nan.waveform.quality.domain.dto.DeviationRowDto;
import com.nan.waveform.quality.domain.dto.HarmonicRowDto;
import com.nan.waveform.quality.domain.dto.PowerQualityDataDto;
import com.nan.waveform.quality.domain.dto.SteadyRowDto;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.util.Units;

/**
 * @author nan chao
 * @since 2026/6/16 10:26
 *
 * poi-tl Word 渲染物理引擎
 */

public class WordReportExporter {
    public static void exportToResponse(Map<String, Object> textDataModel, MultipartFile excelFile, List<MultipartFile> images, String testSite, HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("电能质量测试报告", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".docx");

        //Hutool 流式解析器读取原始 Excel 数据
        ExcelSheetData excelData;
        try (InputStream excelStream = excelFile.getInputStream()) {
            excelData = parseExcelFromStream(excelStream);
        }

        //动态注入 6 条富文本结论所需的动态变量
        List<List<Object>> vData = excelData.getVoltageHarmonicData();
        List<List<Object>> pData = excelData.getPowerData();

        textDataModel.put("volHarmonicConclusion", "2～25次谐波电压中均满足国标要求；");
        textDataModel.put("curHarmonicConclusion", "2～25次谐波电流中均满足国标要求；");

        // 电压偏差动态最大值提取与回填
        double voltage_deviation_up_AB_max = getDoubleValue(vData.get(63).get(2));
        double voltage_deviation_up_BC_max = getDoubleValue(vData.get(63).get(7));
        double voltage_deviation_up_AC_max = getDoubleValue(vData.get(63).get(12));
        double max_voltage_deviation = Math.max(voltage_deviation_up_AB_max, Math.max(voltage_deviation_up_BC_max, voltage_deviation_up_AC_max));
        textDataModel.put("volDevConclusion", String.format("电压偏差：0.00%%～%.2f%%（最大值）满足国标要求；", max_voltage_deviation));
        textDataModel.put("maxVoltageDeviation", formatDouble(max_voltage_deviation, 2));

        // 长时间闪变结论提取
        double long_term_flicker_AB_95 = getDoubleValue(vData.get(61).get(5));
        textDataModel.put("flickerConclusion", String.format("长时间闪变（Plt）：%.2f（95%%值），满足国标要求；", long_term_flicker_AB_95));

        // 三相不平衡度结论提取
        double voltage_unbalance_95 = getDoubleValue(pData.get(16).get(5));
        textDataModel.put("unbalanceConclusion", String.format("三相电压不平衡度：%.2f%%（95%%值），满足国标要求；", voltage_unbalance_95));

        // 频率偏差结论提取
        double frequency_max = getDoubleValue(pData.get(15).get(2));
        textDataModel.put("freqConclusion", String.format("频率偏差：±%.2fHz，满足国标要求。", Math.abs(frequency_max - 50.0)));

        // 3. 给上传的照片发放免死金牌
        if (images != null && !images.isEmpty()) {
            for (int i = 0; i < images.size(); i++) {
                textDataModel.put("image" + (i + 1), "{{image" + (i + 1) + "}}");
            }
        }

        // 4. 驱动 poi-tl 渲染文本和结论
        try (InputStream templateStream = WordReportExporter.class.getResourceAsStream("/templates/input模板v3.docx")) {
            if (templateStream == null) {
                throw new RuntimeException("找不到 Word 模板文件 [input模板v3.docx]");
            }

            XWPFTemplate template = XWPFTemplate.compile(templateStream).render(textDataModel);
            XWPFDocument doc = template.getXWPFDocument();
            List<XWPFTable> tables = doc.getTables();


            if (tables.size() >= 4) {
                fillVoltageHarmonicTable(tables.get(0), excelData.getVoltageHarmonicData());
                fillCurrentHarmonicTable(tables.get(1), excelData.getCurrentHarmonicData());
                fillFrequencyDeviationAndVoltageUnbalanceAndLongTermFlickerTable(tables.get(2), excelData.getVoltageHarmonicData(), excelData.getPowerData());
                fillVoltageDeviationTable(tables.get(3), excelData.getVoltageHarmonicData());
            }


            if (images != null && !images.isEmpty()) {
                String monitorPosition = (testSite == null) ? "" : testSite;
                for (int i = 0; i < images.size(); i++) {
                    String placeholder = "{{image" + (i + 1) + "}}";
                    MultipartFile imageFile = images.get(i);
                    if (imageFile == null || imageFile.isEmpty()) continue;
                    try (InputStream imgStream = imageFile.getInputStream()) {
                        insertImageAndModifyCaption(doc, placeholder, imgStream, detectImageType(imageFile.getOriginalFilename()), 400, 250, monitorPosition);
                    } catch (Exception ignored) {}
                }
            }

            template.write(response.getOutputStream());
            template.close();
        }
    }


    private static ExcelSheetData parseExcelFromStream(InputStream excelStream) {
        ExcelSheetData data = new ExcelSheetData();
        ExcelReader reader = ExcelUtil.getReader(excelStream);
        try {
            reader.setSheet("电压谐波");
            List<List<Object>> voltageData = reader.read();
            processMergedCells(voltageData, reader.getSheet());
            data.getVoltageHarmonicData().addAll(voltageData);

            reader.setSheet("电流谐波");
            List<List<Object>> currentData = reader.read();
            processMergedCells(currentData, reader.getSheet());
            data.getCurrentHarmonicData().addAll(currentData);

            reader.setSheet("功率");
            List<List<Object>> powerData = reader.read();
            processMergedCells(powerData, reader.getSheet());
            data.getPowerData().addAll(powerData);
        } finally {
            reader.close();
        }
        return data;
    }


    private static void processMergedCells(List<List<Object>> sheetData, Sheet sheet) {
        List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
        for (CellRangeAddress region : mergedRegions) {
            int firstRow = region.getFirstRow();
            int lastRow = region.getLastRow();
            int firstCol = region.getFirstColumn();
            int lastCol = region.getLastColumn();
            Object value = sheetData.get(firstRow).get(firstCol);
            for (int row = firstRow; row <= lastRow; row++) {
                for (int col = firstCol; col <= lastCol; col++) {
                    if (row != firstRow || col != firstCol) {
                        if (row < sheetData.size() && col < sheetData.get(row).size()) {
                            sheetData.get(row).set(col, value);
                        }
                    }
                }
            }
        }
    }


    private static void fillVoltageHarmonicTable(XWPFTable table, List<List<Object>> data) {
        double AB_average_fundamental = getDoubleValue(data.get(9).get(3));
        List<Double> AB_average_hruh_list = readColumnRange(data, 3, 10, 33);
        double AB_average_THD = getDoubleValue(data.get(59).get(3));

        double AB_95_fundamental = getDoubleValue(data.get(9).get(5));
        List<Double> AB_95_hruh_list = readColumnRange(data, 5, 10, 33);
        double AB_95_THD = getDoubleValue(data.get(59).get(5));

        double BC_average_fundamental = getDoubleValue(data.get(9).get(8));
        List<Double> BC_average_hruh_list = readColumnRange(data, 8, 10, 33);
        double BC_average_THD = getDoubleValue(data.get(59).get(8));

        double BC_95_fundamental = getDoubleValue(data.get(9).get(10));
        List<Double> BC_95_hruh_list = readColumnRange(data, 10, 10, 33);
        double BC_95_THD = getDoubleValue(data.get(59).get(10));

        double CA_average_fundamental = getDoubleValue(data.get(9).get(13));
        List<Double> CA_average_hruh_list = readColumnRange(data, 13, 10, 33);
        double CA_average_THD = getDoubleValue(data.get(59).get(13));

        double CA_95_fundamental = getDoubleValue(data.get(9).get(15));
        List<Double> CA_95_hruh_list = readColumnRange(data, 15, 10, 33);
        double CA_95_THD = getDoubleValue(data.get(59).get(15));

        List<Double> limit_hruh_list = readColumnRange(data, 17, 10, 33);
        double limit_THD = getDoubleValue(data.get(59).get(17));

        if (table.getRows().size() > 2 && table.getRows().get(2).getTableCells().size() > 2) {
            setCellText(table.getRows().get(2).getCell(1), formatDouble(AB_average_fundamental / 1000, 2));
        }
        for (int i = 0; i < AB_average_hruh_list.size(); i++) {
            if (table.getRows().size() > i + 3 && table.getRows().get(i + 3).getTableCells().size() > 2) {
                setCellText(table.getRows().get(i + 3).getCell(2), formatDouble(AB_average_hruh_list.get(i), 2));
            }
        }
        if (table.getRows().size() > 27 && table.getRows().get(27).getTableCells().size() > 2) {
            setCellText(table.getRows().get(27).getCell(1), formatDouble(AB_average_THD, 2));
        }

        if (table.getRows().size() > 2 && table.getRows().get(2).getTableCells().size() > 3) {
            setCellText(table.getRows().get(2).getCell(2), formatDouble(AB_95_fundamental / 1000, 2));
        }
        for (int i = 0; i < AB_95_hruh_list.size(); i++) {
            if (table.getRows().size() > i + 3 && table.getRows().get(i + 3).getTableCells().size() > 3) {
                setCellText(table.getRows().get(i + 3).getCell(3), formatDouble(AB_95_hruh_list.get(i), 2));
            }
        }
        if (table.getRows().size() > 27 && table.getRows().get(27).getTableCells().size() > 3) {
            setCellText(table.getRows().get(27).getCell(2), formatDouble(AB_95_THD, 2));
        }

        if (table.getRows().size() > 2 && table.getRows().get(2).getTableCells().size() > 4) {
            setCellText(table.getRows().get(2).getCell(3), formatDouble(BC_average_fundamental / 1000, 2));
        }
        for (int i = 0; i < BC_average_hruh_list.size(); i++) {
            if (table.getRows().size() > i + 3 && table.getRows().get(i + 3).getTableCells().size() > 4) {
                setCellText(table.getRows().get(i + 3).getCell(4), formatDouble(BC_average_hruh_list.get(i), 2));
            }
        }
        if (table.getRows().size() > 27 && table.getRows().get(27).getTableCells().size() > 4) {
            setCellText(table.getRows().get(27).getCell(3), formatDouble(BC_average_THD, 2));
        }

        if (table.getRows().size() > 2 && table.getRows().get(2).getTableCells().size() > 5) {
            setCellText(table.getRows().get(2).getCell(4), formatDouble(BC_95_fundamental / 1000, 2));
        }
        for (int i = 0; i < BC_95_hruh_list.size(); i++) {
            if (table.getRows().size() > i + 3 && table.getRows().get(i + 3).getTableCells().size() > 5) {
                setCellText(table.getRows().get(i + 3).getCell(5), formatDouble(BC_95_hruh_list.get(i), 2));
            }
        }
        if (table.getRows().size() > 27 && table.getRows().get(27).getTableCells().size() > 5) {
            setCellText(table.getRows().get(27).getCell(4), formatDouble(BC_95_THD, 2));
        }

        if (table.getRows().size() > 2 && table.getRows().get(2).getTableCells().size() > 6) {
            setCellText(table.getRows().get(2).getCell(5), formatDouble(CA_average_fundamental / 1000, 2));
        }
        for (int i = 0; i < CA_average_hruh_list.size(); i++) {
            if (table.getRows().size() > i + 3 && table.getRows().get(i + 3).getTableCells().size() > 6) {
                setCellText(table.getRows().get(i + 3).getCell(6), formatDouble(CA_average_hruh_list.get(i), 2));
            }
        }
        if (table.getRows().size() > 27 && table.getRows().get(27).getTableCells().size() > 6) {
            setCellText(table.getRows().get(27).getCell(5), formatDouble(CA_average_THD, 2));
        }

        if (table.getRows().size() > 2 && table.getRows().get(2).getTableCells().size() > 7) {
            setCellText(table.getRows().get(2).getCell(6), formatDouble(CA_95_fundamental / 1000, 2));
        }
        for (int i = 0; i < CA_95_hruh_list.size(); i++) {
            if (table.getRows().size() > i + 3 && table.getRows().get(i + 3).getTableCells().size() > 7) {
                setCellText(table.getRows().get(i + 3).getCell(7), formatDouble(CA_95_hruh_list.get(i), 2));
            }
        }
        if (table.getRows().size() > 27 && table.getRows().get(27).getTableCells().size() > 7) {
            setCellText(table.getRows().get(27).getCell(6), formatDouble(CA_95_THD, 2));
        }

        for (int i = 0; i < limit_hruh_list.size(); i++) {
            if (table.getRows().size() > i + 3 && table.getRows().get(i + 3).getTableCells().size() > 8) {
                setCellText(table.getRows().get(i + 3).getCell(8), formatDouble(limit_hruh_list.get(i), 2));
            }
        }
        setCellText(table.getRows().get(2).getCell(7), ("—"));
        setCellText(table.getRows().get(27).getCell(7), formatDouble(limit_THD, 2));
    }


    private static void fillCurrentHarmonicTable(XWPFTable table, List<List<Object>> data) {
        double A_average_fundamental = getDoubleValue(data.get(9).get(3));
        List<Double> A_average_hruh_list = readColumnRange(data, 3, 10, 33);

        double A_95_fundamental = getDoubleValue(data.get(9).get(5));
        List<Double> A_95_hruh_list = readColumnRange(data, 5, 10, 33);

        double B_average_fundamental = getDoubleValue(data.get(9).get(8));
        List<Double> B_average_hruh_list = readColumnRange(data, 8, 10, 33);

        double B_95_fundamental = getDoubleValue(data.get(9).get(10));
        List<Double> B_95_hruh_list = readColumnRange(data, 10, 10, 33);

        double C_average_fundamental = getDoubleValue(data.get(9).get(13));
        List<Double> C_average_hruh_list = readColumnRange(data, 13, 10, 33);

        double C_95_fundamental = getDoubleValue(data.get(9).get(15));
        List<Double> C_95_hruh_list = readColumnRange(data, 15, 10, 33);

        List<Double> limit_hruh_list = readColumnRange(data, 17, 10, 33);

        if (table.getRows().size() > 2 && table.getRows().get(2).getTableCells().size() > 2) {
            setCellText(table.getRows().get(2).getCell(1), formatDouble(A_average_fundamental, 2));
        }
        if (table.getRows().size() > 2 && table.getRows().get(2).getTableCells().size() > 3) {
            setCellText(table.getRows().get(2).getCell(2), formatDouble(A_95_fundamental, 2));
        }
        if (table.getRows().size() > 2 && table.getRows().get(2).getTableCells().size() > 4) {
            setCellText(table.getRows().get(2).getCell(3), formatDouble(B_average_fundamental, 2));
        }
        if (table.getRows().size() > 2 && table.getRows().get(2).getTableCells().size() > 5) {
            setCellText(table.getRows().get(2).getCell(4), formatDouble(B_95_fundamental, 2));
        }
        if (table.getRows().size() > 2 && table.getRows().get(2).getTableCells().size() > 6) {
            setCellText(table.getRows().get(2).getCell(5), formatDouble(C_average_fundamental, 2));
        }
        if (table.getRows().size() > 2 && table.getRows().get(2).getTableCells().size() > 7) {
            setCellText(table.getRows().get(2).getCell(6), formatDouble(C_95_fundamental, 2));
        }

        for (int i = 0; i < 24; i++) {
            if (table.getRows().size() > i + 3 && table.getRows().get(i + 3).getTableCells().size() > 2) {
                setCellText(table.getRows().get(i + 3).getCell(2), formatDouble(A_average_hruh_list.get(i), 2));
            }
            if (table.getRows().size() > i + 3 && table.getRows().get(i + 3).getTableCells().size() > 3) {
                setCellText(table.getRows().get(i + 3).getCell(3), formatDouble(A_95_hruh_list.get(i), 2));
            }
            if (table.getRows().size() > i + 3 && table.getRows().get(i + 3).getTableCells().size() > 4) {
                setCellText(table.getRows().get(i + 3).getCell(4), formatDouble(B_average_hruh_list.get(i), 2));
            }
            if (table.getRows().size() > i + 3 && table.getRows().get(i + 3).getTableCells().size() > 5) {
                setCellText(table.getRows().get(i + 3).getCell(5), formatDouble(B_95_hruh_list.get(i), 2));
            }
            if (table.getRows().size() > i + 3 && table.getRows().get(i + 3).getTableCells().size() > 6) {
                setCellText(table.getRows().get(i + 3).getCell(6), formatDouble(C_average_hruh_list.get(i), 2));
            }
            if (table.getRows().size() > i + 3 && table.getRows().get(i + 3).getTableCells().size() > 7) {
                setCellText(table.getRows().get(i + 3).getCell(7), formatDouble(C_95_hruh_list.get(i), 2));
            }
            if (table.getRows().size() > i + 3 && table.getRows().get(i + 3).getTableCells().size() > 8) {
                setCellText(table.getRows().get(i + 3).getCell(8), formatDouble(limit_hruh_list.get(i), 2));
            }
        }
        setCellText(table.getRows().get(2).getCell(7), ("—"));
    }


    private static void fillFrequencyDeviationAndVoltageUnbalanceAndLongTermFlickerTable(XWPFTable table, List<List<Object>> voltageHarmonicData, List<List<Object>> powerData) {
        double frequency_max = getDoubleValue(powerData.get(15).get(2));
        double frequency_average = getDoubleValue(powerData.get(15).get(3));
        double frequency_min = getDoubleValue(powerData.get(15).get(4));
        double frequency_95 = getDoubleValue(powerData.get(15).get(5));
        String frequency_limit = powerData.get(15).get(17).toString();

        double voltage_unbalance_max = getDoubleValue(powerData.get(16).get(2));
        double voltage_unbalance_average = getDoubleValue(powerData.get(16).get(3));
        double voltage_unbalance_min = getDoubleValue(powerData.get(16).get(4));
        double voltage_unbalance_95 = getDoubleValue(powerData.get(16).get(5));
        double voltage_unbalance_limit = getDoubleValue(powerData.get(16).get(17));

        double long_term_flicker_AB_max = getDoubleValue(voltageHarmonicData.get(61).get(2));
        double long_term_flicker_AB_average = getDoubleValue(voltageHarmonicData.get(61).get(3));
        double long_term_flicker_AB_min = getDoubleValue(voltageHarmonicData.get(61).get(4));
        double long_term_flicker_AB_95 = getDoubleValue(voltageHarmonicData.get(61).get(5));

        double long_term_flicker_BC_max = getDoubleValue(voltageHarmonicData.get(61).get(7));
        double long_term_flicker_BC_average = getDoubleValue(voltageHarmonicData.get(61).get(8));
        double long_term_flicker_BC_min = getDoubleValue(voltageHarmonicData.get(61).get(9));
        double long_term_flicker_BC_95 = getDoubleValue(voltageHarmonicData.get(61).get(10));

        double long_term_flicker_AC_max = getDoubleValue(voltageHarmonicData.get(61).get(12));
        double long_term_flicker_AC_average = getDoubleValue(voltageHarmonicData.get(61).get(13));
        double long_term_flicker_AC_min = getDoubleValue(voltageHarmonicData.get(61).get(14));
        double long_term_flicker_AC_95 = getDoubleValue(voltageHarmonicData.get(61).get(15));

        double long_term_flicker_limit = getDoubleValue(voltageHarmonicData.get(61).get(17));

        setCellText(table.getRow(1).getCell(1), formatDouble(frequency_max - 50, 2));
        setCellText(table.getRow(1).getCell(2), formatDouble(frequency_average - 50, 2));
        setCellText(table.getRow(1).getCell(3), formatDouble(frequency_min - 50, 2));
        setCellText(table.getRow(1).getCell(4), formatDouble(frequency_95 - 50, 2));
        setCellText(table.getRow(1).getCell(5), frequency_limit);

        setCellText(table.getRow(2).getCell(1), formatDouble(voltage_unbalance_max, 2));
        setCellText(table.getRow(2).getCell(2), formatDouble(voltage_unbalance_average, 2));
        setCellText(table.getRow(2).getCell(3), formatDouble(voltage_unbalance_min, 2));
        setCellText(table.getRow(2).getCell(4), formatDouble(voltage_unbalance_95, 2));
        setCellText(table.getRow(2).getCell(5), formatDouble(voltage_unbalance_limit, 2));

        setCellText(table.getRow(3).getCell(2), formatDouble(long_term_flicker_AB_max, 2));
        setCellText(table.getRow(3).getCell(3), formatDouble(long_term_flicker_AB_average, 2));
        setCellText(table.getRow(3).getCell(4), formatDouble(long_term_flicker_AB_min, 2));
        setCellText(table.getRow(3).getCell(5), formatDouble(long_term_flicker_AB_95, 2));
        setCellText(table.getRow(3).getCell(6), formatDouble(long_term_flicker_limit, 2));

        setCellText(table.getRow(4).getCell(2), formatDouble(long_term_flicker_BC_max, 2));
        setCellText(table.getRow(4).getCell(3), formatDouble(long_term_flicker_BC_average, 2));
        setCellText(table.getRow(4).getCell(4), formatDouble(long_term_flicker_BC_min, 2));
        setCellText(table.getRow(4).getCell(5), formatDouble(long_term_flicker_BC_95, 2));
        setCellText(table.getRow(4).getCell(6), formatDouble(long_term_flicker_limit, 2));

        setCellText(table.getRow(5).getCell(2), formatDouble(long_term_flicker_AC_max, 2));
        setCellText(table.getRow(5).getCell(3), formatDouble(long_term_flicker_AC_average, 2));
        setCellText(table.getRow(5).getCell(4), formatDouble(long_term_flicker_AC_min, 2));
        setCellText(table.getRow(5).getCell(5), formatDouble(long_term_flicker_AC_95, 2));
        setCellText(table.getRow(5).getCell(6), formatDouble(long_term_flicker_limit, 2));
    }


    private static void fillVoltageDeviationTable(XWPFTable table, List<List<Object>> voltageHarmonicData) {
        double voltage_deviation_up_AB_max = getDoubleValue(voltageHarmonicData.get(63).get(2));
        double voltage_deviation_up_AB_min = getDoubleValue(voltageHarmonicData.get(63).get(4));
        double voltage_deviation_up_BC_max = getDoubleValue(voltageHarmonicData.get(63).get(7));
        double voltage_deviation_up_BC_min = getDoubleValue(voltageHarmonicData.get(63).get(9));
        double voltage_deviation_up_AC_max = getDoubleValue(voltageHarmonicData.get(63).get(12));
        double voltage_deviation_up_AC_min = getDoubleValue(voltageHarmonicData.get(63).get(14));
        double voltage_deviation_up_limit = getDoubleValue(voltageHarmonicData.get(63).get(17));

        double voltage_deviation_down_AB_max = getDoubleValue(voltageHarmonicData.get(64).get(2));
        double voltage_deviation_down_AB_min = getDoubleValue(voltageHarmonicData.get(64).get(4));
        double voltage_deviation_down_BC_max = getDoubleValue(voltageHarmonicData.get(64).get(7));
        double voltage_deviation_down_BC_min = getDoubleValue(voltageHarmonicData.get(64).get(9));
        double voltage_deviation_down_AC_max = getDoubleValue(voltageHarmonicData.get(64).get(12));
        double voltage_deviation_down_AC_min = getDoubleValue(voltageHarmonicData.get(64).get(14));
        double voltage_deviation_down_limit = Double.parseDouble("-" + voltageHarmonicData.get(64).get(17).toString());

        setCellText(table.getRow(2).getCell(1), formatDouble(voltage_deviation_up_AB_max, 2));
        setCellText(table.getRow(2).getCell(2), formatDouble(voltage_deviation_up_AB_min, 2));
        setCellText(table.getRow(2).getCell(3), formatDouble(voltage_deviation_up_BC_max, 2));
        setCellText(table.getRow(2).getCell(4), formatDouble(voltage_deviation_up_BC_min, 2));
        setCellText(table.getRow(2).getCell(5), formatDouble(voltage_deviation_up_AC_max, 2));
        setCellText(table.getRow(2).getCell(6), formatDouble(voltage_deviation_up_AC_min, 2));
        setCellText(table.getRow(2).getCell(7), formatDouble(voltage_deviation_up_limit, 2));

        setCellText(table.getRow(3).getCell(1), formatDouble(voltage_deviation_down_AB_max, 2));
        setCellText(table.getRow(3).getCell(2), formatDouble(voltage_deviation_down_AB_min, 2));
        setCellText(table.getRow(3).getCell(3), formatDouble(voltage_deviation_down_BC_max, 2));
        setCellText(table.getRow(3).getCell(4), formatDouble(voltage_deviation_down_BC_min, 2));
        setCellText(table.getRow(3).getCell(5), formatDouble(voltage_deviation_down_AC_max, 2));
        setCellText(table.getRow(3).getCell(6), formatDouble(voltage_deviation_down_AC_min, 2));
        setCellText(table.getRow(3).getCell(7), formatDouble(voltage_deviation_down_limit, 2));
    }


    private static class ExcelSheetData {
        private final List<List<Object>> voltageHarmonicData = new ArrayList<>();
        private final List<List<Object>> currentHarmonicData = new ArrayList<>();
        private final List<List<Object>> powerData = new ArrayList<>();
        public List<List<Object>> getVoltageHarmonicData() { return voltageHarmonicData; }
        public List<List<Object>> getCurrentHarmonicData() { return currentHarmonicData; }
        public List<List<Object>> getPowerData() { return powerData; }
    }

    private static List<Double> readColumnRange(List<List<Object>> data, int colIndex, int startRow, int endRow) {
        List<Double> result = new ArrayList<>();
        for (int i = startRow; i <= endRow; i++) {
            result.add(getDoubleValue(data.get(i).get(colIndex)));
        }
        return result;
    }

    private static double getDoubleValue(Object obj) {
        if (obj == null) return 0.0;
        try { return Double.parseDouble(obj.toString()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private static String formatDouble(double value, int scale) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(scale, BigDecimal.ROUND_HALF_UP);
        return bd.toString();
    }

    private static void setCellText(XWPFTableCell cell, String text) {
        if (cell == null) return;
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        while (cell.getParagraphs().size() > 0) { cell.removeParagraph(0); }
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setFontFamily("Times New Roman");
        run.setFontSize(10); // 小五
        run.setText(text == null ? "" : text);
    }

    private static void insertImageAndModifyCaption(XWPFDocument doc, String placeholder, InputStream imageStream, int imageType, int w, int h, String pos) throws Exception {
        List<XWPFParagraph> paragraphs = doc.getParagraphs();
        for (int i = 0; i < paragraphs.size(); i++) {
            XWPFParagraph para = paragraphs.get(i);
            if (para.getText() != null && para.getText().contains(placeholder)) {
                int runCount = para.getRuns().size();
                for (int j = runCount - 1; j >= 0; j--) para.removeRun(j);
                XWPFRun run = para.createRun();
                run.addPicture(imageStream, imageType, "image", Units.toEMU(w), Units.toEMU(h));
                if (i + 1 < paragraphs.size()) {
                    XWPFParagraph capPara = paragraphs.get(i + 1);
                    String orig = capPara.getText();
                    if (orig != null && !orig.trim().startsWith("图1.1")) {
                        int space = orig.indexOf(' ');
                        String cap = (space != -1) ? orig.substring(0, space + 1) + " " + pos + " " + orig.substring(space + 1) : orig + " " + pos;
                        int cCount = capPara.getRuns().size();
                        for (int j = cCount - 1; j >= 0; j--) capPara.removeRun(j);
                        XWPFRun cRun = capPara.createRun();
                        cRun.setText(cap); cRun.setFontFamily("SimSun"); cRun.setFontSize(12);
                    }
                }
                return;
            }
        }
    }

    private static int detectImageType(String filename) {
        if (filename == null) return XWPFDocument.PICTURE_TYPE_PNG;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return XWPFDocument.PICTURE_TYPE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return XWPFDocument.PICTURE_TYPE_JPEG;
        return XWPFDocument.PICTURE_TYPE_PNG;
    }
}
