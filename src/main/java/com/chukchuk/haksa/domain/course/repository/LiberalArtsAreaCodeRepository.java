package com.chukchuk.haksa.domain.course.repository;

import com.chukchuk.haksa.domain.course.model.LiberalArtsAreaCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LiberalArtsAreaCodeRepository extends JpaRepository<LiberalArtsAreaCode, Integer> {

    @Modifying
    @Query(value = """
            INSERT INTO liberal_arts_area_codes (code, area_name, is_active)
            VALUES (:code, :areaName, TRUE)
            ON CONFLICT (code) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("code") Integer code, @Param("areaName") String areaName);
}
