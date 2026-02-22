package com.chukchuk.haksa.domain.professor.service;

import com.chukchuk.haksa.domain.professor.model.Professor;
import com.chukchuk.haksa.domain.professor.repository.ProfessorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessorServiceUnitTests {

    @Mock
    private ProfessorRepository professorRepository;

    @InjectMocks
    private ProfessorService professorService;

    @Test
    @DisplayName("기존 교수가 존재하면 해당 교수를 반환한다")
    void getOrCreate_whenExists_returnsExisting() {
        Professor existing = new Professor("홍길동");
        when(professorRepository.findByProfessorName("홍길동")).thenReturn(Optional.of(existing));

        Professor result = professorService.getOrCreate("홍길동");

        assertThat(result).isSameAs(existing);
        verify(professorRepository, never()).save(any(Professor.class));
    }

    @Test
    @DisplayName("기존 교수가 없으면 새 교수를 생성해 저장한다")
    void getOrCreate_whenMissing_createsAndSaves() {
        Professor saved = new Professor("김교수");
        when(professorRepository.findByProfessorName("김교수")).thenReturn(Optional.empty());
        when(professorRepository.save(any(Professor.class))).thenReturn(saved);

        Professor result = professorService.getOrCreate("김교수");

        assertThat(result).isSameAs(saved);
        verify(professorRepository).save(any(Professor.class));
    }
}
