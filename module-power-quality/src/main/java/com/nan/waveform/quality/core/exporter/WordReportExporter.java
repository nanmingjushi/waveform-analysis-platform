package com.nan.waveform.quality.core.exporter;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Map;

/**
 * @author nan chao
 * @since 2026/6/16 10:26
 *
 * poi-tl Word 渲染物理引擎
 */

public class WordReportExporter {
    public static void exportToResponse(Map<String, Object> dataModel, HttpServletResponse response) throws Exception {
        // 1. 设置跨域与浏览器强制下载 Header
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("电能质量分析审计报告", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".docx");

        // 2. 装配模板渲染策略 (开启严格循环渲染)
        Configure config = Configure.builder().build();

        // 3. 读取 Classpath 下的 Word 物理模板并渲染
        try (InputStream templateStream = WordReportExporter.class.getResourceAsStream("/templates/input模板v2.docx")) {
            if (templateStream == null) {
                throw new RuntimeException("致命错误：资源目录中找不到 Word 模板文件 [input模板v2.docx]");
            }

            // 4. 发动 poi-tl 引擎，将内存 Map 数据以微秒级速度全量注入
            XWPFTemplate template = XWPFTemplate.compile(templateStream, config).render(dataModel);

            // 5. 流式输出，直接打回给前端浏览器的下载槽
            template.write(response.getOutputStream());
            template.close();
        }
    }
}
