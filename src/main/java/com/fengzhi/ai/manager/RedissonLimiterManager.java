package com.fengzhi.ai.manager;

import com.fengzhi.ai.common.ErrorCode;
import com.fengzhi.ai.exception.BusinessException;
import org.redisson.api.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class RedissonLimiterManager {
    @Resource
    private RedissonClient redissonClient;

    public void doRateLimit(String key) {

        // 创建一个名称为user_limiter的限流器，每秒最多访问 2 次
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL, 2, 1, RateIntervalUnit.SECONDS);

        // 每当一个操作来了后，请求一个令牌
        boolean canOp = rateLimiter.tryAcquire(1);
        if (!canOp) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
