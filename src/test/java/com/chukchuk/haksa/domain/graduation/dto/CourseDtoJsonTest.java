package com.chukchuk.haksa.domain.graduation.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CourseDtoJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("liberalAreaCode 값이 있으면 JSON에 포함된다")
    void includesLiberalAreaCodeWhenPresent() throws JsonProcessingException {
        CourseDto dto = new CourseDto(2024, "기독교의 이해", 3, "A+", 10, 7);

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"liberalAreaCode\":7");
    }

    @Test
    @DisplayName("liberalAreaCode 가 null 이면 JSON 응답에서 키가 omit 된다 (@JsonInclude NON_NULL)")
    void omitsLiberalAreaCodeWhenNull() throws JsonProcessingException {
        CourseDto dto = new CourseDto(2023, "자료구조", 3, "A+", 20, null);

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).doesNotContain("liberalAreaCode");
    }

    @Test
    @DisplayName("기존 필드들은 null이어도 JSON 응답에서 유지된다")
    void preservesExistingFieldsAlongsideNullLiberalAreaCode() throws JsonProcessingException {
        CourseDto dto = new CourseDto(2023, "자료구조", null, "A+", 20, null);

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"year\":2023");
        assertThat(json).contains("\"courseName\":\"자료구조\"");
        assertThat(json).contains("\"credits\":null");
        assertThat(json).contains("\"grade\":\"A+\"");
        assertThat(json).contains("\"semester\":20");
        assertThat(json).doesNotContain("liberalAreaCode");
    }
}
