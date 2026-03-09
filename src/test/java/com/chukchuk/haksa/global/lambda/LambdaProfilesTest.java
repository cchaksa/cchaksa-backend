package com.chukchuk.haksa.global.lambda;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class LambdaProfilesTest {

    @AfterEach
    void clearProfilesProperty() {
        System.clearProperty("spring.profiles.active");
    }

    @Test
    void developShadowProfileIncludesDevGroup() {
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(new ClassPathResource("application.yml"));

        Properties properties = yamlPropertiesFactoryBean.getObject();

        assertThat(properties)
                .isNotNull();
        assertThat(properties.getProperty("spring.profiles.group.develop-shadow[0]"))
                .isEqualTo("dev");
    }

    @Test
    void resolveActiveProfilesUsesSystemPropertyBeforeDefault() {
        System.setProperty("spring.profiles.active", "prod,develop-shadow");

        assertThat(LambdaProfiles.resolveActiveProfiles())
                .containsExactly("prod", "develop-shadow");
    }
}
