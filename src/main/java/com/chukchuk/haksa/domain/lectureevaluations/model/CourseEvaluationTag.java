package com.chukchuk.haksa.domain.lectureevaluations.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "course_evaluation_tags",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_evaluation_tags_evaluation_tag",
                        columnNames = {"course_evaluation_id", "tag"}
                )
        },
        indexes = {
                @Index(name = "idx_course_evaluation_tags_evaluation", columnList = "course_evaluation_id")
        }
)
public class CourseEvaluationTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_evaluation_id", nullable = false)
    private CourseEvaluation courseEvaluation;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false, length = 64)
    private LectureEvaluationTag tag;

    public CourseEvaluationTag(CourseEvaluation courseEvaluation, LectureEvaluationTag tag) {
        this.courseEvaluation = courseEvaluation;
        this.tag = tag;
    }
}
