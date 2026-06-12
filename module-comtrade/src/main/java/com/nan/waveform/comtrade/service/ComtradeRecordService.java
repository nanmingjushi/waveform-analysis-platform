package com.nan.waveform.comtrade.service;

import com.nan.waveform.comtrade.domain.entity.ComtradeRecord;
import com.nan.waveform.comtrade.domain.vo.ComtradeRecordListVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/11 15:35
 *
 * comtrade格式录波文件解析业务逻辑层接口
 */

public interface ComtradeRecordService {

    /**
     * 上传并解析 COMTRADE 录波文件，并进行业务数据落库
     * @param cfgFile  前端上传的 .cfg 配置文件
     * @param datFile  前端上传的 .dat 二进制数据文件
     * @param userId   当前操作的用户 ID (从 JWT 拦截器中提取)
     * @return 存储成功后的完整录波元数据记录 (只存储这个文件的“文本标签”和“磁盘指针”。)
     */
    ComtradeRecord uploadAndProcess(MultipartFile cfgFile, MultipartFile datFile, Long userId);

    /**
     * 获取指定用户名下的所有录波解析历史记录列表
     * @param userId 当前登录的用户 ID
     * @return 经过精简转换后的前端表格展示对象(VO)集合
     */
    List<ComtradeRecordListVo> getRecordListByUserId(Long userId);

    /**
     * 根据记录 ID 和用户 ID 获取并校验合法的录波记录 (带水平越权校验)
     * @param id     录波记录主键 ID
     * @param userId 当前登录用户 ID
     * @return 完整的录波记录实体类
     */
    ComtradeRecord getValidatedRecord(Long id, Long userId);

    /**
     * 直接接收已验证的实体，实时读取本地物理文件并高性能转换为 CSV 字节流输出
     * @param record       已经过权限校验的录波记录实体
     * @param outputStream 目标网络输出流
     */
    void downloadCsv(ComtradeRecord record, java.io.OutputStream outputStream);
}
