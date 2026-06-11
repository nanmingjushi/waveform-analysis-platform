package com.nan.waveform.comtrade.service.impl;

import com.nan.waveform.comtrade.core.parser.CfgParser;
import com.nan.waveform.comtrade.core.parser.DatParser;
import com.nan.waveform.comtrade.domain.dto.ComtradeParseResultDto;
import com.nan.waveform.comtrade.domain.entity.ComtradeRecord;
import com.nan.waveform.comtrade.domain.vo.ComtradeRecordListVo;
import com.nan.waveform.comtrade.mapper.ComtradeRecordMapper;
import com.nan.waveform.comtrade.service.ComtradeRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author nan chao
 * @since 2026/6/11 15:35
 *
 * comtrade格式录波文件解析业务逻辑层实现类
 */

@Slf4j
@Service
public class ComtradeRecordServiceImpl implements ComtradeRecordService {

    @Autowired
    private ComtradeRecordMapper comtradeRecordMapper;

    /**
     * 亮点设计：使用 System.getProperty("user.dir") 动态获取当前项目的绝对根目录。
     * 无论项目在 IDE 里启动，还是未来打成 JAR 包在 Linux 服务器上运行，
     * 都会在项目根目录下雷打不动地创建并指向 /uploads/comtrade/ 文件夹，完全实现本地自闭环管理。
     */
    private static final String STORAGE_ROOT = System.getProperty("user.dir") + "/uploads/comtrade/";

    @Override
    @Transactional(rollbackFor = Exception.class) // 电力核心数据落库，强制开启 Spring 声明式事务
    public ComtradeRecord uploadAndProcess(MultipartFile cfgFile, MultipartFile datFile, Long userId) {
        // 1. 强安全前置校验
        if (cfgFile == null || cfgFile.isEmpty() || datFile == null || datFile.isEmpty()) {
            throw new IllegalArgumentException("错误：上传的录波 CFG 或 DAT 文件对象不能为空");
        }

        try {
            // 2. 确保项目根目录下的存储文件夹存在，不存在则原生 NIO 创建
            Path directoryPath = Paths.get(STORAGE_ROOT);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
                log.info("系统在项目根目录下首次成功创建录波专用缓冲存储池: [{}]", STORAGE_ROOT);
            }

            // 3. 产生互不干扰的唯一 UUID
            String fileFingerprint = UUID.randomUUID().toString().replace("-", "");
            String cfgNewName = fileFingerprint + ".cfg";
            String datNewName = fileFingerprint + ".dat";

            // 4. 构建本地文件指针 (强制转换为绝对路径 file 实例，增强 transferTo 的跨系统兼容性)
            File cfgDiskTarget = new File(STORAGE_ROOT + cfgNewName).getAbsoluteFile();
            File datDiskTarget = new File(STORAGE_ROOT + datNewName).getAbsoluteFile();

            // 5. 庞大的波形物理文件原封不动、直接推向服务器本地硬盘
            cfgFile.transferTo(cfgDiskTarget);
            datFile.transferTo(datDiskTarget);
            log.info("用户 [{}] 的大体量波形物理数据在磁盘安全留痕成功! 目标路径: {}", userId, STORAGE_ROOT);

            // 6. 驱动无状态 Core 核心算法引擎，直接在内存中抓取、清洗元数据
            // 6.1 抓取通道元配置
            ComtradeParseResultDto parseResult = CfgParser.parse(cfgFile.getInputStream());
            // 6.2 抓取时间采样点 (此处 DatParser 会解包二进制，但处理完毕后由垃圾回收器 GC 释放，不持久化点数据)
            DatParser.parse(datFile.getInputStream(), parseResult);

            // 7. 组装极轻量级的元数据标签实体（Entity），彻底屏蔽大量数字
            ComtradeRecord record = new ComtradeRecord();
            record.setUserId(userId);
            // 列表展示的文件名保留用户原始上传的名称，提升用户体验
            record.setFileName(cfgFile.getOriginalFilename());
            record.setStationName(parseResult.getStationName());
            record.setDeviceId(parseResult.getDeviceId());
            record.setStartTime(parseResult.getStartTime());
            record.setTriggerTime(parseResult.getTriggerTime());
            record.setAnalogCount(parseResult.getAnalogChannels().size());
            record.setDigitalCount(parseResult.getDigitalChannels().size());
            record.setLineFrequency(BigDecimal.valueOf(parseResult.getLineFrequency()));

            // MySQL 只存这两个轻量级的绝对路径字符串指针
            record.setCfgFilePath(cfgDiskTarget.getPath());
            record.setDatFilePath(datDiskTarget.getPath());

            // 8. 递交给 MyBatis 接口，将这一行文本标签沉淀进数据库
            comtradeRecordMapper.insert(record);
            log.info("用户 [{}] 的录波描述性元数据入库完工! 数据库主键索引 ID = [{}]", userId, record.getId());

            return record;

        } catch (IOException e) {
            log.error("警告：本地 IO 流写入或文件解析链路发生严重阻塞崩溃", e);
            throw new RuntimeException("服务器本地文件处理落库失败: " + e.getMessage());
        }
    }

    @Override
    public List<ComtradeRecordListVo> getRecordListByUserId(Long userId) {
        // 1. 严格按照当前登录的 userId 进行 MySQL 条件检索，确保用户之间数据不可见，实现数据安全隔离
        List<ComtradeRecord> dbRecords = comtradeRecordMapper.selectListByUserId(userId);
        List<ComtradeRecordListVo> frontendVoList = new ArrayList<>();

        // 2. 将包含敏感物理路径的 Entity，安全过滤并剥离，转换为精简的 VO 给前端展示
        for (ComtradeRecord record : dbRecords) {
            ComtradeRecordListVo vo = new ComtradeRecordListVo();
            vo.setId(record.getId());
            vo.setFileName(record.getFileName());
            vo.setStationName(record.getStationName());
            vo.setDeviceId(record.getDeviceId());
            vo.setStartTime(record.getStartTime());
            vo.setTriggerTime(record.getTriggerTime());
            vo.setAnalogCount(record.getAnalogCount());
            vo.setDigitalCount(record.getDigitalCount());
            vo.setLineFrequency(record.getLineFrequency());
            vo.setCreateTime(record.getCreateTime());
            frontendVoList.add(vo);
        }

        log.info("用户权限隔离校验成功：成功为操作员 [{}] 呈递了 [{}] 条历史录波卡片数据", userId, frontendVoList.size());
        return frontendVoList;
    }

}
