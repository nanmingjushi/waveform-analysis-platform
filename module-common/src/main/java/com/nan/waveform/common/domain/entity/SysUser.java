package com.nan.waveform.common.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author nan chao
 * @since 2026/6/11 10:48
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysUser {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private Date createTime;

}
