package com.fengzhi.ai.interceptor;

import com.fengzhi.ai.common.BaseContext;
import com.fengzhi.ai.common.ErrorCode;
import com.fengzhi.ai.constant.JwtClaimsConstant;
import com.fengzhi.ai.constant.UserConstant;
import com.fengzhi.ai.exception.BusinessException;
import com.fengzhi.ai.exception.ThrowUtils;
import com.fengzhi.ai.properties.JwtProperties;
import com.fengzhi.ai.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getTokenName());
        if(token.equals("null")){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"未登录!");
        }

        //2、校验令牌
        try {
            Claims claims = JwtUtils.parseJWT(jwtProperties.getSecretKey(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            BaseContext.setCurrentId(userId);
            log.info("当前用户id: {}", userId);

            String userKey = UserConstant.USER_KEY + userId;
            Long expire = stringRedisTemplate.getExpire(userKey, TimeUnit.MINUTES);

            if(expire < 0){
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"登录过期!");
            }else if(expire < 5){
                // 更新登录用户过期时间
                stringRedisTemplate.expire(userKey, UserConstant.EXPIRE_TIME, TimeUnit.MINUTES);
            }
            log.info("剩余过期时间为: {}分钟", expire);

            //3、通过，放行
            return true;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"登录过期!");
        }
    }
}
