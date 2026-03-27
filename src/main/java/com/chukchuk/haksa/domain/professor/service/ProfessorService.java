package com.chukchuk.haksa.domain.professor.service;

import com.chukchuk.haksa.domain.professor.model.Professor;
import com.chukchuk.haksa.domain.professor.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessorService {
    private final ProfessorRepository professorRepository;

    @Transactional
    public Professor getOrCreate(String professorName) {
        return professorRepository.findByProfessorName(professorName)
                .orElseGet(() -> {
                    Professor newProfessor = new Professor(professorName);
                    return professorRepository.save(newProfessor);
                });
    }

    @Transactional
    public Map<String, Professor> getOrCreateAll(Collection<String> professorNames) {
        if (professorNames == null || professorNames.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> uniqueNames = professorNames.stream()
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

        List<Professor> toCreate = uniqueNames.stream()
                .filter(name -> !result.containsKey(name))
                .map(Professor::new)
                .toList();

        if (!toCreate.isEmpty()) {
            List<Professor> saved = professorRepository.saveAll(toCreate);
            for (Professor professor : saved) {
                result.put(professor.getProfessorName(), professor);
            }
        }

        return result;
    }
}
