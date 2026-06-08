// 외국어 인증 기준 학과 매핑 Repository 조회 조건을 검증하는 테스트
package com.chukchuk.haksa.domain.graduation.repository;

import com.chukchuk.haksa.domain.graduation.model.DepartmentLanguageCertPolicyMapping;
import com.chukchuk.haksa.domain.graduation.model.LanguageCertPolicyGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DepartmentLanguageCertPolicyMappingRepositoryTests {

    @Autowired
    private DepartmentLanguageCertPolicyMappingRepository mappingRepository;

    @Autowired
    private LanguageCertPolicyGroupRepository policyGroupRepository;

    @Test
    @DisplayName("학과 코드와 입학년도가 적용되는 매핑만 조회한다")
    void findApplicableMappingsFiltersByDepartmentCodeAndAdmissionYear() {
        LanguageCertPolicyGroup group = policyGroupRepository.save(LanguageCertPolicyGroup.create(
                "ICT_DATA_SCIENCE",
                "ICT융합대학 데이터과학부",
                null
        ));
        mappingRepository.save(DepartmentLanguageCertPolicyMapping.verified(
                "2000511",
                2018,
                2020,
                group,
                "18~20 기준"
        ));
        DepartmentLanguageCertPolicyMapping current = mappingRepository.save(
                DepartmentLanguageCertPolicyMapping.verified(
                        "2000511",
                        2021,
                        9999,
                        group,
                        "21 이후 기준"
                )
        );
        mappingRepository.save(DepartmentLanguageCertPolicyMapping.verified(
                "2000757",
                2025,
                9999,
                group,
                "다른 학과 코드"
        ));

        List<DepartmentLanguageCertPolicyMapping> result =
                mappingRepository.findApplicableMappings("2000511", 2025);

        assertThat(result).containsExactly(current);
    }
}
