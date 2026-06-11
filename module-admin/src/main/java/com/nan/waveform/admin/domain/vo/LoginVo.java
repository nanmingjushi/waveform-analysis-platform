package com.nan.waveform.admin.domain.vo;

import lombok.Data;

/**
 * @author nan chao
 * @since 2026/6/11 16:48
 *
 * 用户登录成功后返回给前端的数据载体 (View Object)
 */

@Data
public class LoginVo {
    private String token;    // JWT 访问令牌
    private String realName; // 用户真实姓名
}
