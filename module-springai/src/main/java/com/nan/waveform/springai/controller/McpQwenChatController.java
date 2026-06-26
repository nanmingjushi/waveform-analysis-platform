package com.nan.waveform.springai.controller;

import com.nan.waveform.springai.dto.PowerReportOutputDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @author nan chao
 * @since 2026/6/25
 *
 * 国网电能质量报告自动生成（集成MCP）
 */
@RestController
@RequestMapping("/ai/mcp/qwen")
public class McpQwenChatController {

    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider;

    public McpQwenChatController(OpenAiChatModel chatModel, ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.toolCallbackProvider = toolCallbackProvider;
    }

    @PostMapping("/generate-power-report")
    public PowerReportOutputDTO generatePowerReport(@RequestParam("file") MultipartFile file) throws IOException {

        // 建立暂存文件
        File tempFile = File.createTempFile("waveform_mcp_", ".xls");
        file.transferTo(tempFile);
        String tempExcelAbsolutePath = tempFile.getAbsolutePath();

        try {
            // 从 Provider 获取所有工具回调
            ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
            if (callbacks == null || callbacks.length == 0) {
                throw new RuntimeException("MCP 工具回调为空，请检查 MCP 客户端是否正常连接并获取到工具列表。");
            }

            // 查找目标工具
            ToolCallback excelTool = null;
            for (ToolCallback callback : callbacks) {
                String toolName = callback.getToolDefinition().name();
                System.out.println("====== 发现可用 MCP 工具: " + toolName + " ======");
                if (toolName.endsWith("analyze_power_quality_excel") || toolName.contains("analyze_power_quality_excel")) {
                    excelTool = callback;
                    break;
                }
            }

            if (excelTool == null) {
                throw new RuntimeException("未找到名为 'analyze_power_quality_excel' 的工具，请确认 Python 端已注册该工具。");
            }

            // 转义 Windows 路径，组装 JSON 参数
            String jsonArgs = String.format("{\"file_path\":\"%s\"}", tempExcelAbsolutePath.replace("\\", "\\\\"));

            // Java 直接驱动 Python MCP 工具执行，返回原始 Excel 文本
            String rawExcelText = excelTool.call(jsonArgs);

            if (!StringUtils.hasText(rawExcelText)) {
                throw new RuntimeException("工具调用返回了空内容，请检查 Python 脚本执行是否正常。");
            }

            String systemPrompt = """
                    你是一个驻守在国家电网的电能质量报告生成专家。  
                    工具已成功解析录波仪 Excel 数据，并以结构化 JSON 形式返回了所有必需指标。  
                    你的任务：  
                    1. 将输入数据中的数组内容直接映射到输出 DTO，确保数组长度和顺序正确。  
                    2. 若某记录的 `conclusion` 为空，且 `limit` 不为空，则用对应的 95% 值（或最大/最小值）与 `limit` 比较，若超出则写 "超标"，否则写 "合格"。  
                    3. 只输出符合给定 Schema 的纯 JSON，不要包含 Markdown 代码块。  
                    """;

            String userPrompt = "以下是工具返回的完整结构化数据（JSON）：\n\n" + rawExcelText + "\n\n请按上述要求输出报告。";


            // 调用大模型生成最终 DTO
            return this.chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .advisors(new SimpleLoggerAdvisor())
                    .call()
                    .entity(PowerReportOutputDTO.class);

        } finally {
            // 阅后删除
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}