// CodeRabbit 자동 리뷰 정책과 경로별 검토 범위를 검증하는 테스트
package com.chukchuk.haksa.global.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CodeRabbitConfigTest {

    private static Map<String, Object> config;
    private static Map<String, Object> reviews;
    private static Map<String, Object> autoReview;

    @BeforeAll
    static void loadConfig() throws Exception {
        try (InputStream inputStream = Files.newInputStream(Path.of(".coderabbit.yaml"))) {
            config = map(new Yaml().load(inputStream));
        }
        reviews = map(config.get("reviews"));
        autoReview = map(reviews.get("auto_review"));
    }

    @Test
    void 백엔드_자동_리뷰_정책을_유지한다() {
        assertThat(config.get("language")).isEqualTo("ko-KR");
        assertThat(reviews.get("profile")).isEqualTo("chill");
        assertThat(reviews.get("poem")).isEqualTo(false);
        assertThat(reviews.get("in_progress_fortune")).isEqualTo(false);
        assertThat(autoReview.get("enabled")).isEqualTo(true);
        assertThat(autoReview.get("auto_incremental_review")).isEqualTo(true);
        assertThat(autoReview.get("drafts")).isEqualTo(false);
        assertThat(autoReview.get("base_branches"))
                .isEqualTo(List.of("^main$", "^release/.*$"));
    }

    @Test
    void 백엔드_위험_영역별_리뷰_지침을_유지한다() {
        Object rawPathInstructions = reviews.get("path_instructions");
        assertThat(rawPathInstructions).isInstanceOf(List.class);
        List<Map<String, Object>> pathInstructions = list(rawPathInstructions);
        List<String> paths = pathInstructions.stream()
                .map(instruction -> (String) instruction.get("path"))
                .toList();

        assertThat(paths).containsExactly(
                "src/main/java/com/chukchuk/haksa/global/security/**",
                "src/main/java/com/chukchuk/haksa/domain/auth/**",
                "src/main/java/com/chukchuk/haksa/domain/**",
                "src/main/java/com/chukchuk/haksa/application/**",
                "src/main/java/com/chukchuk/haksa/application/portal/**",
                "src/main/java/com/chukchuk/haksa/infrastructure/portal/**",
                "src/main/resources/db/migration/**",
                "src/main/java/com/chukchuk/haksa/global/logging/**",
                "src/main/java/com/chukchuk/haksa/global/exception/**",
                "src/test/**"
        );
        assertThat(pathInstructions)
                .allSatisfy(instruction -> assertThat(instruction.get("instructions"))
                        .isInstanceOf(String.class)
                        .asString()
                        .isNotBlank());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
