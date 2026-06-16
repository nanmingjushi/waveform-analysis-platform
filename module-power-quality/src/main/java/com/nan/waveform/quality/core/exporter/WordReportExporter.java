package com.nan.waveform.quality.core.exporter;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.nan.waveform.quality.domain.dto.HarmonicRowDto;
import com.nan.waveform.quality.domain.dto.PowerQualityDataDto;
import com.nan.waveform.quality.domain.dto.SteadyRowDto;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/**
 * @author nan chao
 * @since 2026/6/16 10:26
 *
 * poi-tl Word 渲染物理引擎
 */

public class WordReportExporter {
    public static void exportToResponse(Map<String, Object> textDataModel, PowerQualityDataDto tableDataModel, HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("电能质量测试数据报告", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".docx");

        try (InputStream templateStream = WordReportExporter.class.getResourceAsStream("/templates/input模板v2.docx")) {
            if (templateStream == null) {
                throw new RuntimeException("找不到 Word 模板文件 [input模板v2.docx]");
            }

            // 1. 先用 poi-tl 渲染普通文本变量 (如 {{client}}, 结论等)
            XWPFTemplate template = XWPFTemplate.compile(templateStream).render(textDataModel);

            // 2. 获取底层的原生 XWPFDocument 文档对象 (致敬您 Demo 的核心逻辑)
            XWPFDocument document = template.getXWPFDocument();
            List<XWPFTable> tables = document.getTables();

            if (tables.size() >= 4) {
                // =============== 表1.1 谐波电压统计表 ===============
                fillHarmonicTable(tables.get(0), tableDataModel.getVolHarmonics());

                // =============== 表1.2 谐波电流统计表 ===============
                fillHarmonicTable(tables.get(1), tableDataModel.getCurHarmonics());

                // =============== 表1.3 频率偏差、三相不平衡度、闪变 ===============
                XWPFTable steadyTable = tables.get(2);
                fillSteadyTable(steadyTable, "频率偏差(Hz)", tableDataModel.getFrequency());
                fillSteadyTable(steadyTable, "三相电压不平衡度(%)", tableDataModel.getUnbalance());
                fillSteadyTable(steadyTable, "长时间闪变(Plt)", tableDataModel.getFlicker());

                // =============== 表1.4 电压偏差 ===============
                XWPFTable devTable = tables.get(3);
                // 电压偏差表：参数, 最大值, 最小值, 限值
                if (tableDataModel.getVoltageDeviation() != null) {
                    XWPFTableRow row = devTable.createRow();
                    setCellText(row, 0, "电压偏差(%)");
                    setCellText(row, 1, tableDataModel.getVoltageDeviation().getMaxVal());
                    setCellText(row, 2, tableDataModel.getVoltageDeviation().getMinVal());
                    setCellText(row, 3, tableDataModel.getVoltageDeviation().getLimitVal());
                }
            }

            // 3. 直接输出
            template.write(response.getOutputStream());
            template.close();
        }
    }

    /**
     * 原生操纵行：填充谐波表
     */
    private static void fillHarmonicTable(XWPFTable table, List<HarmonicRowDto> harmonics) {
        if (harmonics == null) return;
        for (HarmonicRowDto dto : harmonics) {
            XWPFTableRow row = table.createRow();
            setCellText(row, 0, dto.getOrder());
            setCellText(row, 1, dto.getAbAvg());
            setCellText(row, 2, dto.getAb95());
            setCellText(row, 3, dto.getBcAvg());
            setCellText(row, 4, dto.getBc95());
            setCellText(row, 5, dto.getCaAvg());
            setCellText(row, 6, dto.getCa95());
            setCellText(row, 7, dto.getLimitVal());
        }
    }

    /**
     * 原生操纵行：填充稳态表
     */
    private static void fillSteadyTable(XWPFTable table, String name, SteadyRowDto dto) {
        XWPFTableRow row = table.createRow();
        setCellText(row, 0, name);
        if (dto != null) {
            setCellText(row, 1, dto.getMaxVal());
            setCellText(row, 2, dto.getAvgVal());
            setCellText(row, 3, dto.getMinVal());
            setCellText(row, 4, dto.getVal95());
            setCellText(row, 5, dto.getLimitVal());
        } else {
            setCellText(row, 1, "--"); setCellText(row, 2, "--");
            setCellText(row, 3, "--"); setCellText(row, 4, "--"); setCellText(row, 5, "--");
        }
    }

    /**
     * 安全设置单元格文字防空指针
     */
    private static void setCellText(XWPFTableRow row, int index, String text) {
        if (row.getCell(index) == null) {
            row.addNewTableCell();
        }
        row.getCell(index).setText(text == null ? "--" : text);
    }
}
