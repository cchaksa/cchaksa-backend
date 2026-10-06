package com.chukchuk.haksa.domain.course.repository;

import com.chukchuk.haksa.domain.course.model.LiberalArtsAreaCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 포털의 원시 교양 영역 코드를 표준 교양 영역 정보로 매핑해 조회한다. */
@Repository
public interface LiberalArtsAreaCodeRepository extends JpaRepository<LiberalArtsAreaCode, Integer> {

  /**
   * 교양 영역 코드가 없을 때만 활성 기준정보를 추가한다.
   *
   * @param code 교양 영역 코드
   * @param areaName 교양 영역 이름
   * @return 추가된 행 수
   */
  @Modifying
  @Query(
      value =
          "INSERT INTO liberal_arts_area_codes (code, area_name, is_active)\n"
              + "VALUES (:code, :areaName, TRUE)\n"
              + "ON CONFLICT (code) DO NOTHING",
      nativeQuery = true)
  int insertIfAbsent(@Param("code") Integer code, @Param("areaName") String areaName);
}
