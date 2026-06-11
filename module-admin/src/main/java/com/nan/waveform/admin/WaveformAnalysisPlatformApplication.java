package com.nan.waveform.admin;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author nan chao
 * @since 2026/6/9 21:02
 */

/**
 * 试验录波快速解析平台 - 主启动类
 */
@SpringBootApplication(scanBasePackages = "com.nan.waveform") // 确保 Spring 能够扫描到所有模块的 Service/Controller
@MapperScan("com.nan.waveform.**.mapper") // 强制 MyBatis 扫描所有模块下的 mapper 接口
public class WaveformAnalysisPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(WaveformAnalysisPlatformApplication.class, args);
        System.out.println("=================================================");
        System.out.println("(♥◠‿◠)ﾉﾞ  试验录波快速解析平台启动成功！ ⚡  ");
        System.out.println("=================================================");
    }
}
