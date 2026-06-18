package com.nan.waveform.springai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author nan chao
 * @since 2026/6/18 16:11
 */

@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatClient chatClient;

    // 名字必须严格对应 Config 里的方法名 ollamaChatClient，Spring 会自动按名称匹配注入
    public ChatController(ChatClient ollamaChatClient) {
        this.chatClient = ollamaChatClient;
    }

    /**
     * 接口 1：普通同步返回 (阻塞)
     * 适合不需要流式展示的后台业务场景，大模型全部想好后一次性吐出所有文本
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String prompt) {
        return chatClient.prompt().user(prompt).call().content();
    }

    /**
     * 接口 2：流式返回 (SSE - Server-Sent Events)
     * 适合前端对话框。实时流式输出
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String prompt){
        return chatClient.prompt().user(prompt).stream().content();
    }
}
