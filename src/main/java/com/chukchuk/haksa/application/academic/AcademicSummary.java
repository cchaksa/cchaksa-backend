package com.chukchuk.haksa.application.academic;

public class AcademicSummary {
    private Integer totalAttemptedCredits;  // 신청 학점
    private Integer totalEarnedCredits;     // 취득 학점
    private Double cumulativeGpa;           // 누적 GPA
    private Double percentile;              // 학기 백분위

    // 생성자
    public AcademicSummary(Integer totalAttemptedCredits, Integer totalEarnedCredits,
                           Double cumulativeGpa, Double percentile) {
        this.totalAttemptedCredits = totalAttemptedCredits;
        this.totalEarnedCredits = totalEarnedCredits;
        this.cumulativeGpa = cumulativeGpa;
        this.percentile = percentile;
    }

    // Getter 메서드들
    public Integer getTotalAttemptedCredits() {
        return totalAttemptedCredits;
    }

    public Integer getTotalEarnedCredits() {
        return totalEarnedCredits;
    }

    public Double getCumulativeGpa() {
        return cumulativeGpa;
    }

    public Double getPercentile() {
        return percentile;
    }

    // Setter 메서드들
    public void setTotalAttemptedCredits(Integer totalAttemptedCredits) {
        this.totalAttemptedCredits = totalAttemptedCredits;
    }

    public void setTotalEarnedCredits(Integer totalEarnedCredits) {
        this.totalEarnedCredits = totalEarnedCredits;
    }

    public void setCumulativeGpa(Double cumulativeGpa) {
        this.cumulativeGpa = cumulativeGpa;
    }

    public void setPercentile(Double percentile) {
        this.percentile = percentile;
    }

    @Override
    public String toString() {
        return "AcademicSummary{" +
                "totalAttemptedCredits=" + totalAttemptedCredits +
                ", totalEarnedCredits=" + totalEarnedCredits +
                ", cumulativeGpa=" + cumulativeGpa +
                ", percentile=" + percentile +
                '}';
    }
}