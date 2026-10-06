package com.chukchuk.haksa.domain.academic.record.repository;

import java.util.List;

/** 학생 수강 내역을 JDBC 일괄 연산으로 저장하는 계약을 정의한다. */
public interface StudentCourseBulkRepository {

  /**
   * 학생 수강 과목 행을 배치로 저장한다.
   *
   * @param rows 한 번의 배치로 삽입하거나 갱신할 수강 내역 행
   */
  void insertAll(List<StudentCourseBulkRow> rows);
}
