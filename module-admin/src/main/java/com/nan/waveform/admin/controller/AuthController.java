package com.nan.waveform.admin.controller;

import cn.hutool.crypto.SecureUtil;
import com.nan.waveform.admin.domain.vo.LoginVo;
import com.nan.waveform.admin.mapper.SysUserMapper;
import com.nan.waveform.common.domain.entity.SysUser;
import com.nan.waveform.common.result.Result;
import com.nan.waveform.common.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * @author nan chao
 * @since 2026/6/11 11:03
 */

@Tag(name = "认证模块", description = "用户登录与权限相关接口") // 这一行会显示在 Knife4j 左侧菜单栏
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Operation(summary = "用户登录", description = "输入用户名和密码获取 JWT Token")
    @PostMapping("/login")
    public Result<LoginVo> login(@RequestBody Map<String, String> loginParam) {
        String username = loginParam.get("username");
        String password = loginParam.get("password");

        // 1. 查询用户
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            // 优雅地返回错误响应，不再通过 map.put 拼装
            return Result.error(400, "用户不存在");
        }

        // 2. 验证密码 (后端进行 MD5 摘要比对)
        String md5Pwd = SecureUtil.md5(password);
        if (!user.getPassword().equals(md5Pwd)) {
            return Result.error(400, "密码错误");
        }

        // 3. 颁发令牌
        String token = jwtUtils.createToken(user.getId(), user.getUsername());

        // 4. 强类型组装返回数据对象 (VO)
        LoginVo loginVo = new LoginVo();
        loginVo.setToken(token);
        loginVo.setRealName(user.getRealName());

        log.info("用户 [{}] 成功通过凭证校验，系统已安全颁发 JWT 令牌", username);

        // 优雅地返回成功响应，完美契合前端 Axios 拦截器的解析规范
        return Result.success(loginVo);
    }
}
