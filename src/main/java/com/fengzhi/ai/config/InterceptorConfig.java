package com.fengzhi.ai.config;


import com.fengzhi.ai.interceptor.CorsInterceptor;
import com.fengzhi.ai.interceptor.JwtTokenInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
public class InterceptorConfig extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenInterceptor jwtTokenInterceptor;

    @Autowired
    private CorsInterceptor corsInterceptor;


    /**
     * 注册自定义拦截器
     *
     * @param registry
     */
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册跨域拦截器...");
        registry.addInterceptor(corsInterceptor)
                .addPathPatterns("/**");;
        log.info("开始注册jwt拦截器...");
        registry.addInterceptor(jwtTokenInterceptor)
//                .addPathPatterns("/user/**","/chart/**")
                .excludePathPatterns("/user/login","/user/register");
    }

    /**
     * 设置静态资源映射，主要是访问接口文档（html、js、css）
     * @param registry
     */
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始设置静态资源映射...");
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }


}
