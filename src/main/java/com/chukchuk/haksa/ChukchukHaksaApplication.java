package com.chukchuk.haksa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** 척척학사 백엔드 애플리케이션의 실행 진입점을 제공한다. */
@SpringBootApplication
@EnableJpaAuditing
public class ChukchukHaksaApplication extends SpringBootServletInitializer {

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(ChukchukHaksaApplication.class);
  }

  /**
   * 척척학사 백엔드 애플리케이션을 실행한다.
   *
   * @param args 애플리케이션 실행 인자
   */
  public static void main(String[] args) {
    SpringApplication.run(ChukchukHaksaApplication.class, args);
  }
}
