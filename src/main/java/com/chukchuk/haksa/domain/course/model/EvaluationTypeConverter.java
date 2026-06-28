package com.chukchuk.haksa.domain.course.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class EvaluationTypeConverter implements AttributeConverter<EvaluationType, String> {

    @Override
    public String convertToDatabaseColumn(EvaluationType attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public EvaluationType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return EvaluationType.UNKNOWN;
        }
        try {
            return EvaluationType.valueOf(dbData.trim());
        } catch (IllegalArgumentException ex) {
            return EvaluationType.UNKNOWN;
        }
    }
}
