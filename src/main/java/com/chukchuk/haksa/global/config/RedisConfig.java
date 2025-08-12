package com.chukchuk.haksa.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    // application.yml 또는 .env에서 Redis 접속 정보 주입
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.username:}")
    private String redisUsername;

    /**
     * RedisTemplate Bean 등록
     * - key, value 모두 문자열로 직렬화
     * - Redis 연결 팩토리를 주입받아 동작
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());   // key: String
        template.setValueSerializer(new StringRedisSerializer()); // value: String
        return template;
    }

    /**
     * RedisConnectionFactory 수동 구성
     * - Render Redis는 rediss:// (SSL) + 인증 비밀번호만 지원 (username 사용 불가)
     * - Spring Boot 3.1+에서는 username 자동 설정되므로 명시적으로 처리 필요
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setPassword(RedisPassword.of(redisPassword));
        config.setUsername(redisUsername);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .useSsl() // rediss:// URL 대응: SSL 사용 필수
                .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }
}