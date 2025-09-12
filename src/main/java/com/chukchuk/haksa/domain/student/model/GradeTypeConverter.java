package com.chukchuk.haksa.domain.student.model;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class GradeTypeConverter implements AttributeConverter<GradeType, String> {

    @Override
    public String convertToDatabaseColumn(GradeType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public GradeType convertToEntityAttribute(String dbData) {
        for (GradeType type : GradeType.values()) {
            if (type.getValue().equals(dbData)) {
                return type;
            }
        }
        throw new CommonException(ErrorCode.INVALID_GRADE_TYPE);
    }
}