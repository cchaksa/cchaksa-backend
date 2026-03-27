package com.chukchuk.haksa.global.lambda;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LambdaProfilesTest {

    @AfterEach
    void clearProfilesProperty() {
        System.clearProperty("spring.profiles.active");
    }

    @Test
    @SuppressWarnings("unchecked")
    void developShadowProfileIncludesDevGroup() throws Exception {
        List<String> developShadowProfiles;
        try (InputStream inputStream = Files.newInputStream(Path.of("src/main/resources/application.yml"))) {
            Map<String, Object> yamlMap = new Yaml().load(inputStream);
            Map<String, Object> spring = (Map<String, Object>) yamlMap.get("spring");
            Map<String, Object> profiles = (Map<String, Object>) spring.get("profiles");
            Map<String, Object> group = (Map<String, Object>) profiles.get("group");
            developShadowProfiles = (List<String>) group.get("develop-shadow");
        }

        assertThat(developShadowProfiles)
                .isNotNull()
                .contains("dev");
    }

    @Test
    void resolveActiveProfilesUsesSystemPropertyBeforeDefault() {
        System.setProperty("spring.profiles.active", "prod,develop-shadow");

        assertThat(LambdaProfiles.resolveActiveProfiles())
                .containsExactly("prod", "develop-shadow");
    }
}
