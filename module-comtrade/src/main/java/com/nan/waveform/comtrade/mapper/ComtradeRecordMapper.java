package com.nan.waveform.comtrade.mapper;

import com.nan.waveform.comtrade.domain.entity.ComtradeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author nan chao
 * @since 2026/6/11 15:46
 *
 * 录波解析记录数据访问层接口
 */

@Mapper
public interface ComtradeRecordMapper {
    /**
     * 插入单条录波解析历史记录
     * @param record 录波记录实体类
     * @return 影响的行数
     */
    int insert(ComtradeRecord record);

    /**
     * 根据用户 ID 查询其名下的所有录波解析历史记录 (实现用户数据隔离)
     * @param userId 上传者用户ID
     * @return 历史记录列表
     */
    List<ComtradeRecord> selectListByUserId(@Param("userId") Long userId);

    /**
     * 根据主键 ID 查询单条记录的详细元数据 (用于后续下载文件或重新算谐波)
     * @param id 主键ID
     * @return 录波记录实体
     */
    ComtradeRecord selectById(@Param("id") Long id);
}
