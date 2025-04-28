package com.anran.highthumb.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {  


    // 第一个 Bean 是将 redisTemplate 的序列化器设置为 Json 序列化和字符串序列化，这样数据在管理工具中的可读性更强，更利于调试
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();  
        template.setConnectionFactory(connectionFactory);  

        // 使用 Jackson2JsonRedisSerializer 序列化值  
        ObjectMapper objectMapper = new ObjectMapper();  
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // Key 使用 String 序列化  
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);  
        template.setHashKeySerializer(new StringRedisSerializer());  
        template.setHashValueSerializer(serializer);  

        template.afterPropertiesSet();  
        return template;  
    }  

    // 第二个是将 Spring Session 的序列化方式改为 Json 序列化，如果在引入依赖时没有引入 SpringSession ，则这一步可以忽略。
    @Bean  
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        // 让 Spring Session 使用 JSON 方式存储  
        return new GenericJackson2JsonRedisSerializer();
    }  

}