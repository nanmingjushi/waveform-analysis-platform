package com.nan.waveform.admin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * @author nan chao
 * @since 2026/6/9 21:02
 */

/**
 * 试验录波快速解析平台 - 主启动类
 */
@SpringBootApplication(scanBasePackages = "com.nan")
public class WaveformAnalysisPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(WaveformAnalysisPlatformApplication.class, args);
        System.out.println("=================================================");
        System.out.println("(♥◠‿◠)ﾉﾞ  试验录波快速解析平台启动成功！ ⚡  ");
        System.out.println("=================================================");
    }
}
