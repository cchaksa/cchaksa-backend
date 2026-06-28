package com.chukchuk.haksa.domain.course.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationTypeConverterTest {

    private final EvaluationTypeConverter converter = new EvaluationTypeConverter();

    @Test
    void convertToEntityAttribute_whenUnknownValue_returnsUnknown() {
        assertThat(converter.convertToEntityAttribute("GRADE")).isEqualTo(EvaluationType.UNKNOWN);
    }

    @Test
    void convertToEntityAttribute_whenBlankValue_returnsUnknown() {
        assertThat(converter.convertToEntityAttribute(" ")).isEqualTo(EvaluationType.UNKNOWN);
    }

    @Test
    void convertToDatabaseColumn_whenNull_returnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }
}
