package com.nan.waveform.admin.controller;

import cn.hutool.crypto.SecureUtil;
import com.nan.waveform.admin.mapper.SysUserMapper;
import com.nan.waveform.common.domain.entity.SysUser;
import com.nan.waveform.common.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author nan chao
 * @since 2026/6/11 11:03
 */

@Tag(name = "认证模块", description = "用户登录与权限相关接口") // 这一行会显示在 Knife4j 左侧菜单栏
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Operation(summary = "用户登录", description = "输入用户名和密码获取 JWT Token") // 这一行会显示在具体接口上
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginParam) {
        String username = loginParam.get("username");
        String password = loginParam.get("password");

        Map<String, Object> result = new HashMap<>();

        // 1. 查询用户
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            result.put("code", 400);
            result.put("msg", "用户不存在");
            return result;
        }

        // 2. 验证密码 (前端发来明文，后端进行 MD5 摘要比对，符合数据库初始数据的格式)
        String md5Pwd = SecureUtil.md5(password);
        if (!user.getPassword().equals(md5Pwd)) {
            result.put("code", 400);
            result.put("msg", "密码错误");
            return result;
        }

        // 3. 颁发令牌
        String token = jwtUtils.createToken(user.getId(), user.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("realName", user.getRealName());

        result.put("code", 200);
        result.put("msg", "登录成功");
        result.put("data", data);
        return result;
    }
}
