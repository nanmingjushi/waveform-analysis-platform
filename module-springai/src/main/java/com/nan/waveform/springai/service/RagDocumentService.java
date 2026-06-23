package com.nan.waveform.springai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/22 21:02
 * RAG 知识库文档 ETL 管道服务
 */

@Service
public class RagDocumentService {
    private final VectorStore vectorStore;
    private final ResourceLoader resourceLoader;

    // 通过构造器注入组件
    public RagDocumentService(VectorStore vectorStore, ResourceLoader resourceLoader) {
        this.vectorStore = vectorStore;
        this.resourceLoader = resourceLoader;
    }

    /**
     * 1：直接接收 Resource 对象
     * 专门用于支持前端真正的 MultipartFile 网页文件上传
     */
    public void importPdfToVectorStore(Resource resource) {
        // 1. 1.0.3 正式版标准的 PDF 读取器可以直接接收 Resource 流，不需要关心文件来自磁盘还是内存
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);

        // 2. 实例化最前沿的文本切片器
        TokenTextSplitter splitter = new TokenTextSplitter();

        // 3. 执行 ETL 转换：提取 PDF 原始段落 -> 智能切碎成 Document 碎片卡片
        List<Document> rawDocuments = pdfReader.get();
        List<Document> splitDocuments = splitter.apply(rawDocuments);

        // 4. 让向量库接收这批碎片（自动调用阿里云百炼 text-embedding-v2 进行计算并存入内存）
        this.vectorStore.accept(splitDocuments);
    }

    /**
     * 2：保留原有的本地磁盘/类路径读取方式
     * 内部直接调用方法
     * * @param filePath 支持 "file:D:/docs/xxx.pdf" 或 "classpath:docs/xxx.pdf"
     */
    public void importPdfToVectorStore(String filePath) {
        // 通过 Spring 资源加载器定位文件
        Resource resource = this.resourceLoader.getResource(filePath);
        // 直接无缝送入核心 RAG 管道
        this.importPdfToVectorStore(resource);
    }
}
