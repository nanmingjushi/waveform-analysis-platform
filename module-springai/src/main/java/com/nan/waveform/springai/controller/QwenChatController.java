package com.nan.waveform.springai.controller;

import com.nan.waveform.springai.service.RagDocumentService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * @author nan chao
 * @since 2026/6/18 16:54
 */


@RestController
@RequestMapping("/ai/qwen")
public class QwenChatController {

    private final ChatClient qwenChatClient;
    private final VectorStore vectorStore;
    private final RagDocumentService ragDocumentService;

    public QwenChatController(ChatClient qwenChatClient, VectorStore vectorStore, RagDocumentService ragDocumentService) {
        this.qwenChatClient = qwenChatClient;
        this.vectorStore = vectorStore;
        this.ragDocumentService = ragDocumentService;
    }

    //普通返回
    @GetMapping("/chat")
    public String qwenChat(
            @RequestParam String prompt,
            @RequestParam(required = false) String chatId,
            @RequestParam(required = false, defaultValue = "false") boolean useRag) { //useRag 是否开启 RAG 知识库模式

        //如果 chatId 为空，自动归入默认全局会话空间，保证记忆切面正常运作
        String finalChatId = StringUtils.hasText(chatId) ? chatId : "default_chat_session";

        var spec=this.qwenChatClient
                .prompt()
                .user(prompt)
                .advisors(s -> s.param(CONVERSATION_ID, finalChatId));
        //是否启用RAG
        if (useRag) {
            spec.advisors(QuestionAnswerAdvisor.builder(vectorStore).build());
        }
        return spec
                .call()
                .content();
    }

    //流式返回
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> qwenStream(
            @RequestParam String prompt,
            @RequestParam(required = false) String chatId,
            @RequestParam(required = false, defaultValue = "false") boolean useRag) {

        //如果 chatId 为空，自动归入默认全局会话空间，保证记忆切面正常运作
        String finalChatId = StringUtils.hasText(chatId) ? chatId : "default_chat_session";

        var spec=this.qwenChatClient
                .prompt()
                .user(prompt)
                .advisors(s -> s.param(CONVERSATION_ID, finalChatId));
        //是否启用RAG
        if (useRag) {
            spec.advisors(QuestionAnswerAdvisor.builder(vectorStore).build());
        }
        return spec
                .stream()
                .content();
    }

    /**
     * 文件上传接口
     * 前端在这个窗口点击“上传文件”图标时，直接调用此接口灌入背景知识
     */
    @PostMapping("/load")
    public String loadPdf(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return "上传的文件不能为空！";
            }
            InputStreamResource resource = new InputStreamResource(file.getInputStream());
            ragDocumentService.importPdfToVectorStore(resource);
            return "PDF 知识库 [" + file.getOriginalFilename() + "] 成功解析并注入当前会话空间！";
        } catch (Exception e) {
            return "文档导入失败，原因: " + e.getMessage();
        }
    }
}


