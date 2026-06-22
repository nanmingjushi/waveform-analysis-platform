package com.nan.waveform.springai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

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
    public String qwenChat(@RequestParam String prompt,@RequestParam(required = false) String chatId) {

        //如果 chatId 为空，自动归入默认全局会话空间，保证记忆切面正常运作
        String finalChatId = StringUtils.hasText(chatId) ? chatId : "default_chat_session";

        return this.qwenChatClient
                .prompt()
                .user(prompt)
                .advisors(spec -> spec.param(CONVERSATION_ID,finalChatId))
                .call()
                .content();
    }

    //流式
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> qwenStream(@RequestParam String prompt,@RequestParam(required = false) String chatId) {

        //如果 chatId 为空，自动归入默认全局会话空间，保证记忆切面正常运作
        String finalChatId = StringUtils.hasText(chatId) ? chatId : "default_chat_session";

        return this.qwenChatClient
                .prompt()
                .user(prompt)
                .advisors(spec -> spec.param(CONVERSATION_ID,finalChatId))
                .stream()
                .content();
    }
}


