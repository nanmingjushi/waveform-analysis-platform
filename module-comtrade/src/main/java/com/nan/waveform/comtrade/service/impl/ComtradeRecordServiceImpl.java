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
import java.io.InputStream;
import java.io.OutputStream;
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

    private static final String STORAGE_ROOT = System.getProperty("user.dir") + "/uploads/comtrade/";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ComtradeRecord uploadAndProcess(MultipartFile cfgFile, MultipartFile datFile, Long userId) {
        if (cfgFile == null || cfgFile.isEmpty() || datFile == null || datFile.isEmpty()) {
            throw new IllegalArgumentException("错误：上传的录波 CFG 或 DAT 文件对象不能为空");
        }

        try {
            // 1. 确保项目根目录下的存储文件夹存在
            Path directoryPath = Paths.get(STORAGE_ROOT);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // 2. 产生互不干扰的唯一 UUID
            String fileFingerprint = UUID.randomUUID().toString().replace("-", "");
            String cfgNewName = fileFingerprint + ".cfg";
            String datNewName = fileFingerprint + ".dat";

            // 3. 构建本地物理文件目标
            File cfgDiskTarget = new File(STORAGE_ROOT + cfgNewName).getAbsoluteFile();
            File datDiskTarget = new File(STORAGE_ROOT + datNewName).getAbsoluteFile();

            // 4. 物理文件切落盘（执行后 Tomcat 临时目录下的原始文件会被无情销毁）
            cfgFile.transferTo(cfgDiskTarget);
            datFile.transferTo(datDiskTarget);
            log.info("用户 [{}] 的大体量波形物理数据在磁盘安全留痕成功!", userId);

            // 🌟开启带有自动资源释放的 try-with-resources 块
            // 直接从我们已经安全落盘的物理文件拉出纯净的文件输入流
            ComtradeParseResultDto parseResult;
            try (InputStream cfgInputStream = Files.newInputStream(cfgDiskTarget.toPath());
                 InputStream datInputStream = Files.newInputStream(datDiskTarget.toPath())) {

                // 驱动无状态算法引擎解析真实落盘文件，彻底规避 Tomcat 临时文件找不到的血案
                parseResult = CfgParser.parse(cfgInputStream);
                DatParser.parse(datInputStream, parseResult);
            }

            // 6. 组装持久化 Entity 实体对象
            ComtradeRecord record = new ComtradeRecord();
            record.setUserId(userId);
            record.setFileName(cfgFile.getOriginalFilename());
            record.setStationName(parseResult.getStationName());
            record.setDeviceId(parseResult.getDeviceId());
            record.setStartTime(parseResult.getStartTime());
            record.setTriggerTime(parseResult.getTriggerTime());
            record.setAnalogCount(parseResult.getAnalogChannels().size());
            record.setDigitalCount(parseResult.getDigitalChannels().size());
            record.setLineFrequency(BigDecimal.valueOf(parseResult.getLineFrequency()));
            record.setCfgFilePath(cfgDiskTarget.getPath());
            record.setDatFilePath(datDiskTarget.getPath());

            // 7. 沉淀进 MySQL
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
        List<ComtradeRecord> dbRecords = comtradeRecordMapper.selectListByUserId(userId);
        List<ComtradeRecordListVo> frontendVoList = new ArrayList<>();

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
        return frontendVoList;
    }


    @Override
    public ComtradeRecord getValidatedRecord(Long id, Long userId) {
        // 1. 从数据库中捞出这条记录的轻量级元数据标签
        ComtradeRecord record = comtradeRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("错误：未找到该对应的录波记录");
        }

        // 2. 纵深防御：严格校验数据所属权，防止恶意水平越权行为
        if (!record.getUserId().equals(userId)) {
            log.warn("安全警报：用户 [{}] 企图越权操作属于用户 [{}] 的录波数据！", userId, record.getUserId());
            throw new RuntimeException("对不起，您无权操作该录波文件数据");
        }

        return record;
    }

    @Override
    public void downloadCsv(ComtradeRecord record, java.io.OutputStream outputStream) {
        // 3. 提取物理磁盘指针
        java.io.File cfgFile = new java.io.File(record.getCfgFilePath());
        java.io.File datFile = new java.io.File(record.getDatFilePath());

        if (!cfgFile.exists() || !datFile.exists()) {
            log.error("物理文件丢失！磁盘路径可能被损坏。CFG: {}, DAT: {}", record.getCfgFilePath(), record.getDatFilePath());
            throw new RuntimeException("服务器本地物理原始录波文件已丢失，请重新上传解析");
        }

        try {
            // 4. 二次流化高效解析：直接从我们自己纳管的物理文件中拉出流
            ComtradeParseResultDto parseResult;
            try (java.io.InputStream cfgIn = java.nio.file.Files.newInputStream(cfgFile.toPath());
                 java.io.InputStream datIn = java.nio.file.Files.newInputStream(datFile.toPath())) {

                // 驱动无状态算法引擎
                parseResult = CfgParser.parse(cfgIn);
                DatParser.parse(datIn, parseResult);
            }

            // 5. 驱动算法层的 CsvExporter 转换引擎，将数据流式压入网络输出流
            com.nan.waveform.comtrade.core.exporter.CsvExporter.exportToCsv(outputStream, parseResult);
            log.info("录波记录 ID [{}] 成功实时转化为标准的 CSV 文本流并投递完毕", record.getId());

        } catch (IOException e) {
            log.error("在将录波物理文件流式转换为 CSV 吐出时发生严重 IO 异常", e);
            throw new RuntimeException("实时导出 CSV 文件失败: " + e.getMessage());
        }
    }
}
