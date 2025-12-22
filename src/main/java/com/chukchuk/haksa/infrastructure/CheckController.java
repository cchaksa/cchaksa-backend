package com.chukchuk.haksa.infrastructure;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class CheckController {

    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    @GetMapping("/sentry-test")
    public void sentryTest() {
        throw new RuntimeException("SENTRY_TEST_DEV");
    }
}
