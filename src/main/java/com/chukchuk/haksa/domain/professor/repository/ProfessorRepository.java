package com.chukchuk.haksa.domain.professor.repository;

import com.chukchuk.haksa.domain.professor.model.Professor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfessorRepository extends JpaRepository<Professor, Long> {
    Optional<Professor> findByProfessorName(String name);

    List<Professor> findByProfessorNameIn(Collection<String> names);
}
