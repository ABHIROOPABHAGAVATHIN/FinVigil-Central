package com.finvigil.transaction.service;

import com.finvigil.transaction.dto.VelocityStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

@Service
public class RedisVelocityService {

    private static final Logger log = LoggerFactory.getLogger(RedisVelocityService.class);

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.aml.velocity-window-seconds:60}")
    private int windowSeconds;

    public RedisVelocityService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public VelocityStats recordAndGetVelocity(String customerUuid, BigDecimal amount) {
        if (stringRedisTemplate == null) {
            return new VelocityStats(1, amount);
        }

        String countKey = "velocity:count:" + customerUuid;
        String amountKey = "velocity:amount:" + customerUuid;

        try {
            Long currentCount = stringRedisTemplate.opsForValue().increment(countKey, 1);
            if (currentCount != null && currentCount == 1) {
                stringRedisTemplate.expire(countKey, Duration.ofSeconds(windowSeconds));
            }

            Double currentAmount = stringRedisTemplate.opsForValue().increment(amountKey, amount.doubleValue());
            if (currentCount != null && currentCount == 1) {
                stringRedisTemplate.expire(amountKey, Duration.ofSeconds(windowSeconds));
            }

            int count = (currentCount != null) ? currentCount.intValue() : 1;
            BigDecimal totalAmount = (currentAmount != null)
                    ? BigDecimal.valueOf(currentAmount).setScale(2, RoundingMode.HALF_UP)
                    : amount;

            log.debug("Updated Redis velocity for customer={}: count={}, totalAmount={}", customerUuid, count, totalAmount);
            return new VelocityStats(count, totalAmount);

        } catch (Exception e) {
            log.warn("Redis unavailable for velocity calculation for customer {}: {}. Falling back to single transaction window.",
                    customerUuid, e.getMessage());
            return new VelocityStats(1, amount);
        }
    }

    public VelocityStats getCurrentVelocity(String customerUuid) {
        if (stringRedisTemplate == null) {
            return new VelocityStats(0, BigDecimal.ZERO);
        }

        String countKey = "velocity:count:" + customerUuid;
        String amountKey = "velocity:amount:" + customerUuid;

        try {
            String countStr = stringRedisTemplate.opsForValue().get(countKey);
            String amountStr = stringRedisTemplate.opsForValue().get(amountKey);

            int count = (countStr != null) ? Integer.parseInt(countStr) : 0;
            BigDecimal amount = (amountStr != null) ? new BigDecimal(amountStr) : BigDecimal.ZERO;

            return new VelocityStats(count, amount);
        } catch (Exception e) {
            log.warn("Error reading Redis velocity for customer {}: {}", customerUuid, e.getMessage());
            return new VelocityStats(0, BigDecimal.ZERO);
        }
    }
}
