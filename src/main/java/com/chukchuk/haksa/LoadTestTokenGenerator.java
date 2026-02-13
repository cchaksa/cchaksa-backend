package com.chukchuk.haksa;

import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.security.service.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * [부하 테스트 전용] 테스트 유저들의 JWT 토큰을 JSON 형식으로 추출하는 유틸리티
 */
@Component
@Profile("dev") // 개발 환경에서만 동작하도록 제한하세
public class LoadTestTokenGenerator implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private JwtProvider jwtProvider;

    @Override
    public void run(String... args) {
        // "test_user"가 포함된 이메일을 가진 유저들을 조회하네
        List<User> testUsers = userRepository.findAll().stream()
                .filter(u -> u.getEmail().contains("test_user"))
                .toList();

        System.out.println("--- k6 users.json START ---");
        System.out.println("[");
        for (int i = 0; i < testUsers.size(); i++) {
            User user = testUsers.get(i);

            // 자네 프로젝트의 JwtProvider 구조에 맞춰 호출하게나
            String token = jwtProvider.createAccessToken(
                    user.getId().toString(),
                    user.getEmail(),
                    "USER" // Role 설정
            );

            System.out.printf("  {\"token\": \"%s\"}%s\n",
                    token, (i == testUsers.size() - 1 ? "" : ","));
        }
        System.out.println("]");
        System.out.println("--- k6 users.json END ---");
    }
}