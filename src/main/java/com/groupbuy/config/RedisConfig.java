package com.groupbuy.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 扣减库存 Lua 脚本：
     * KEYS[1] = 库存 key
     * ARGV[1] = 扣减数量
     * 返回值：-1 key 不存在（未预热）、-2 库存不足、>=0 剩余库存
     * <p>
     * 用 Lua 是因为「判断 + 扣减」必须是原子操作，否则并发下会超卖。
     */
    @Bean
    public DefaultRedisScript<Long> deductStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local stock = redis.call('get', KEYS[1])
                if stock == false then
                    return -1
                end
                if tonumber(stock) < tonumber(ARGV[1]) then
                    return -2
                end
                return redis.call('decrby', KEYS[1], ARGV[1])
                """);
        return script;
    }

    /**
     * 回补库存 Lua 脚本（订单取消时使用）
     */
    @Bean
    public DefaultRedisScript<Long> restoreStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local stock = redis.call('get', KEYS[1])
                if stock == false then
                    return -1
                end
                return redis.call('incrby', KEYS[1], ARGV[1])
                """);
        return script;
    }
}
