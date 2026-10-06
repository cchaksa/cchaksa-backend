package com.chukchuk.haksa.domain.professor.service;

import com.chukchuk.haksa.domain.professor.model.Professor;
import com.chukchuk.haksa.domain.professor.repository.ProfessorRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 교수 이름을 기준으로 교수를 조회하거나 새로 저장한다. */
@Service
@RequiredArgsConstructor
public class ProfessorService {
  private final ProfessorRepository professorRepository;

  /**
   * 이름이 같은 교수를 반환하고, 없으면 새 교수를 저장한다.
   *
   * @param professorName professor 이름
   * @return 이름이 같으면 기존 교수, 없으면 새로 저장한 교수
   */
  @Transactional
  public Professor getOrCreate(String professorName) {
    return professorRepository
        .findByProfessorName(professorName)
        .orElseGet(
            () -> {
              Professor newProfessor = new Professor(professorName);
              return professorRepository.save(newProfessor);
            });
  }

  /**
   * 교수 이름을 중복 제거해 조회하고 없는 교수는 일괄 저장한다.
   *
   * @param professorNames professor names
   * @return 교수 이름과 재사용하거나 새로 저장한 교수의 대응표
   */
  @Transactional
  public Map<String, Professor> getOrCreateAll(Collection<String> professorNames) {
    if (professorNames == null || professorNames.isEmpty()) {
      return Collections.emptyMap();
    }

    Set<String> uniqueNames =
        professorNames.stream()
            .filter(name -> name != null && !name.isBlank())
            .collect(Collectors.toSet());
    if (uniqueNames.isEmpty()) {
      return Collections.emptyMap();
    }

    List<Professor> existing = professorRepository.findByProfessorNameIn(uniqueNames);
    Map<String, Professor> result = new HashMap<>();
    for (Professor professor : existing) {
      result.put(professor.getProfessorName(), professor);
    }

    List<Professor> toCreate =
        uniqueNames.stream().filter(name -> !result.containsKey(name)).map(Professor::new).toList();

    if (!toCreate.isEmpty()) {
      List<Professor> saved = professorRepository.saveAll(toCreate);
      for (Professor professor : saved) {
        result.put(professor.getProfessorName(), professor);
      }
    }

    return result;
  }
}
