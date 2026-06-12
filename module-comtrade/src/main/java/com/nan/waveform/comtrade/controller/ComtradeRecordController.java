package com.nan.waveform.comtrade.controller;

import com.nan.waveform.common.result.Result;
import com.nan.waveform.comtrade.domain.entity.ComtradeRecord;
import com.nan.waveform.comtrade.domain.vo.ComtradeRecordListVo;
import com.nan.waveform.comtrade.service.ComtradeRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/11 15:34
 *
 * 录波文件上传、历史记录查询、触发解析、CSV下载入口
 */

@Tag(name = "comtrade格式录波文件", description = "处理COMTRADE格式录波文件")
@Slf4j
@RestController
@RequestMapping("/api/comtrade")
public class ComtradeRecordController {

    @Autowired
    private ComtradeRecordService comtradeRecordService;

    /**
     * 前端传参：FormData 形式，包含键名为 cfgFile 和 datFile 的两个文件对象
     */
    @Operation(summary = "上传并解析comtrade录波文件", description = "必须同时提交成对的、同名的 .cfg 配置文件与 .dat 二进制数据文件")
    @PostMapping("/upload")
    public Result<ComtradeRecord> uploadComtradeFiles(
            @RequestParam("cfgFile") MultipartFile cfgFile,
            @RequestParam("datFile") MultipartFile datFile,
            HttpServletRequest request) {

        // 从 JWT 拦截器注入的 request 域中，安全提取当前操作员的唯一 userId，实现数据天然隔离
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            log.warn("安全警报：未检测到合法的操作员用户上下文令牌！");
            return Result.error(401, "未登录或登录凭证已失效");
        }

        log.info("接收到操作员 [{}] 的comtrade录波上传请求. CFG: [{}], DAT: [{}]",
                userId, cfgFile.getOriginalFilename(), datFile.getOriginalFilename());

        // 调用业务层执行：落盘留痕 -> 算法解析 -> 标签落库
        ComtradeRecord savedRecord = comtradeRecordService.uploadAndProcess(cfgFile, datFile, userId);

        // 规范化包装，体面返回
        return Result.success(savedRecord);
    }

    /**
     * 获取当前登录用户名下的所有录波历史列表
     */
    @Operation(summary = "获取历史COMTRADE录波解析记录列表", description = "严格根据当前登录的 Token 身份，隔离检索其名下的comtrade波形历史数据")
    @GetMapping("/list")
    public Result<List<ComtradeRecordListVo>> fetchUserRecordList(HttpServletRequest request) {

        // 安全提取 userId上下文
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录或登录凭证已失效");
        }

        // 业务层抓取并过滤敏感路径后的 VO 集合
        List<ComtradeRecordListVo> historyVos = comtradeRecordService.getRecordListByUserId(userId);

        return Result.success(historyVos);
    }


    /**
     * 导出接口：传入录波记录的数据库 ID，系统自动进行后端流式转换并触发浏览器下载
     */
    @Operation(summary = "一键导出并下载 CSV 文件", description = "导出的 CSV 文件将与原始上传的 CFG/DAT 文件完全同名")
    @GetMapping("/download/{id}")
    public void downloadConvertedCsv(
            @org.springframework.web.bind.annotation.PathVariable("id") Long id,
            jakarta.servlet.http.HttpServletResponse response,
            jakarta.servlet.http.HttpServletRequest request) throws IOException {

        // 1. 安全抓取操作员上下文
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            response.sendError(401, "未登录或登录凭证已失效");
            return;
        }

        try {
            // 2. 首先调用新接口，获取并校验合法的数据库记录元数据
            ComtradeRecord record = comtradeRecordService.getValidatedRecord(id, userId);

            // 核心命名魔法：获取库里的原始文件名 (如 2023-06-02_10-00-02_sta_SH5522.CFG)
            String originalFileName = record.getFileName();
            String downloadFileName = "waveform_record_" + id + ".csv"; // 兜底名

            if (originalFileName != null && originalFileName.contains(".")) {
                // 掐头去尾：裁掉最后一个点后面的所有字母，强行拼接上小写的 .csv
                downloadFileName = originalFileName.substring(0, originalFileName.lastIndexOf(".")) + ".csv";
            }

            // 4. 告诉浏览器这是一个标准的附件下载响应，并强制指定 GBK 编码（确保用户用 Excel 双击打开时不出现乱码）
            response.setContentType("text/csv;charset=GBK");
            response.setHeader("Content-Disposition", "attachment; filename=" + downloadFileName);

            // 5. 将网络响应原生字节流递给 Service 层进行高能流式灌注
            comtradeRecordService.downloadCsv(record, response.getOutputStream());

        } catch (Exception e) {
            log.error("下载 CSV 文件在网络管道层发生意外崩溃", e);
            if (!response.isCommitted()) {
                response.reset();
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"导出 CSV 失败：" + e.getMessage() + "\",\"data\":null}");
            }
        }
    }


}
