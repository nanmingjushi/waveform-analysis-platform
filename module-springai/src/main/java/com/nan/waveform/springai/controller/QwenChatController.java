package com.nan.waveform.springai.controller;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author nan chao
 * @since 2026/6/18 16:54
 */


@RestController
@RequestMapping("/ai/qwen") //
public class QwenChatController {
    private final ChatClient qwenChatClient;

    // 名字必须严格对应 Config 里的方法名 qwenChatClient
    public QwenChatController(ChatClient qwenChatClient) {
        this.qwenChatClient = qwenChatClient;
    }

    //普通
    @GetMapping("/chat")
    public String qwenChat(@RequestParam String prompt) {
        return this.qwenChatClient.prompt().user(prompt).call().content();
    }

    //流式
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> qwenStream(@RequestParam String prompt) {
        return this.qwenChatClient.prompt().user(prompt).stream().content();
    }
}


