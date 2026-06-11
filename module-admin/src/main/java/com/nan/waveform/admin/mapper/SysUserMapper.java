package com.nan.waveform.admin.mapper;

import com.nan.waveform.common.domain.entity.SysUser;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author nan chao
 * @since 2026/6/11 10:51
 */

@Mapper
public interface SysUserMapper {

    /**
     * 根据用户名查询用户
     */
    SysUser selectByUsername(@Param("username") String username);
}
