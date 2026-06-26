package com.nan.waveform.springai.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author nan chao
 * @since 2026/6/18 17:09
 *
 * Spring AI 多模型统一装配中心（集成 会话日志，会话记忆）
 */

@Configuration
public class SpringAiConfig {


    //会话记忆
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)  //保留最近20条会话记忆
                .build();
    }

    /**
     * 1. 专门装配本地 Ollama (DeepSeek) 的 ChatClient 实例
     */
    @Bean
    public ChatClient ollamaChatClient(OllamaChatModel ollamaChatModel, ChatMemory chatMemory) {
        return ChatClient.builder(ollamaChatModel)
                .defaultSystem("你是一个电力系统专家，擅长解答关于试验录波文件快速解析的问题。")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(), // 加会话日志，普通对话和流式对话都会被打印
                        MessageChatMemoryAdvisor.builder(chatMemory).build()  //加会话记忆
                )
                .build();
    }

    /**
     * 2. 专门装配云端百炼 (Qwen3.7-Plus) 的 ChatClient 实例
     */
    @Bean
    public ChatClient qwenChatClient(OpenAiChatModel openAiChatModel, ChatMemory chatMemory) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("你是一个电力系统专家，擅长解答关于试验录波文件快速解析的问题。")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()  //加会话记忆
                )  // 加会话日志，普通对话和流式对话都会被打印
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


    /**
     * 基于百炼的嵌入模型，构建独立的 VectorStore 实例
     */
    @Bean
    public VectorStore vectorStore(OpenAiEmbeddingModel openAiEmbeddingModel) {
        return SimpleVectorStore.builder(openAiEmbeddingModel).build();
    }
}
