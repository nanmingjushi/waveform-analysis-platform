package com.nan.waveform.springai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author nan chao
 * @since 2026/6/18 17:09
 *
 * Spring AI 多模型统一装配中心
 */

@Configuration
public class SpringAiConfig {

    /**
     * 1. 专门装配本地 Ollama (DeepSeek) 的 ChatClient 实例
     */
    @Bean
    public ChatClient ollamaChatClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel)
                .defaultSystem("你是一个电力系统专家，擅长解答关于试验录波文件快速解析的问题。")
                .build();
    }

    /**
     * 2. 专门装配云端百炼 (Qwen3.7-Plus) 的 ChatClient 实例
     */
    @Bean
    public ChatClient qwenChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("你是一个电力系统专家，擅长解答关于试验录波文件快速解析的问题。")
                .build();
    }

    /**
     * 3. 手动提供一个默认的 Builder 并赋予最高优先级 (@Primary)
     */
    @Bean
    @Primary
    public ChatClient.Builder defaultChatClientBuilder(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel);
    }
}
