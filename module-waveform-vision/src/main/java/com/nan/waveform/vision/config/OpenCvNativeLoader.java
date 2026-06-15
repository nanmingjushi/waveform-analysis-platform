package com.nan.waveform.vision.config;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.springframework.context.annotation.Configuration;

/**
 * @author nan chao
 * @since 2026/6/15 14:13
 *
 * OpenCV 本地 C++ 动态链接库安全加载器
 * 整个平台项目启动时单例加载一次，杜绝多线程高并发访问时的内存指针死锁
 */

@Configuration
public class OpenCvNativeLoader {
    static {
        try {
            Loader.load(opencv_java.class);
            System.out.println("====== [waveform-vision] OpenCV native libraries successfully loaded via JavaCPP. ======");
        } catch (Throwable e) {
            throw new RuntimeException("Fatal Error: Failed to load OpenCV native libraries", e);
        }
    }
}
