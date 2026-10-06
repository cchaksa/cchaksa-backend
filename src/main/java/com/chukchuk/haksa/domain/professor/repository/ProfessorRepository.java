package com.chukchuk.haksa.domain.professor.repository;

import com.chukchuk.haksa.domain.professor.model.Professor;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 교수 이름 하나 또는 이름 집합으로 교수를 조회하는 저장소다. */
@Repository
public interface ProfessorRepository extends JpaRepository<Professor, Long> {
  /**
   * 이름이 일치하는 교수를 조회한다.
   *
   * @param name 이름
   * @return 이름이 일치하는 교수가 있으면 포함한 선택값
   */
  Optional<Professor> findByProfessorName(String name);

  /**
   * 주어진 이름 중 하나와 일치하는 교수를 조회한다.
   *
   * @param names 조회할 교수 이름 모음
   * @return 주어진 이름 중 하나와 일치하는 교수 목록
   */
  List<Professor> findByProfessorNameIn(Collection<String> names);
}
