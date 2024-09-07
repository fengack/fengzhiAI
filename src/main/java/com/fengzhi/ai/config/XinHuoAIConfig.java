package com.fengzhi.ai.config;

import io.github.briqt.spark4j.SparkClient;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;

/**
 * SparkAi配置
 * @version 1.0
 **/
@Configuration
@ConfigurationProperties(prefix = "xunfei.client")
@Data
public class XinHuoAIConfig {
    private String appid;
    private String apiSecret;
    private String apiKey;
    @Bean
    public SparkClient sparkClient(){
        SparkClient sparkClient = new SparkClient();
        sparkClient.apiKey = apiKey;
        sparkClient.appid = appid;
        sparkClient.apiSecret = apiSecret;
        return sparkClient;
    }
}
