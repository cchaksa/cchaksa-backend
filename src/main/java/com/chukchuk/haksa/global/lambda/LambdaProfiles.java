package com.chukchuk.haksa.global.lambda;

import java.util.Arrays;

public final class LambdaProfiles {

    static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";
    static final String SPRING_PROFILES_ACTIVE_ENV = "SPRING_PROFILES_ACTIVE";
    static final String DEFAULT_ACTIVE_PROFILE = "develop-shadow";

    private LambdaProfiles() {
    }

    public static String[] resolveActiveProfiles() {
        String configuredProfiles = System.getProperty(SPRING_PROFILES_ACTIVE);
        if (configuredProfiles == null || configuredProfiles.isBlank()) {
            configuredProfiles = System.getenv(SPRING_PROFILES_ACTIVE_ENV);
        }
        if (configuredProfiles == null || configuredProfiles.isBlank()) {
            configuredProfiles = DEFAULT_ACTIVE_PROFILE;
        }

        return Arrays.stream(configuredProfiles.split(","))
                .map(String::trim)
                .filter(profile -> !profile.isEmpty())
                .toArray(String[]::new);
    }
}
