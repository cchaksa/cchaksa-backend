package com.chukchuk.haksa.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Token 관련
    TOKEN_INVALID_FORMAT("T01", "ID 토큰 형식이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_NO_MATCHING_KEY("T02", "일치하는 공개키가 없습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_PARSE_ERROR("T03", "ID 토큰 검증 중 오류가 발생했습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("T04", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID_ISS("T05", "유효하지 않은 iss 입니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID_AUD("T06", "유효하지 않은 aud 입니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID_AUD_FORMAT("T07", "aud 클레임 형식이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID_NONCE("T08", "유효하지 않은 nonce 입니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_HASH_ERROR("T09", "SHA-256 해싱 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    TOKEN_INVALID("T10", "토큰이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_MISMATCH("T11", "RefreshToken이 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("T12", "RefreshToken이 존재하지 않습니다.", HttpStatus.UNAUTHORIZED),

    // User 관련
    USER_NOT_FOUND("U01", "해당 사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    STUDENT_ACADEMIC_RECORD_NOT_FOUND("U02", "해당 학생의 학적 정보가 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    STUDENT_NOT_FOUND("S01", "해당 학생이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    INVALID_TARGET_GPA("S02", "유효하지 않은 목표 학점입니다.", HttpStatus.BAD_REQUEST),
    STUDENT_ID_REQUIRED("S03", "Student ID는 필수입니다.", HttpStatus.BAD_REQUEST),

    // 학업 관련
    SEMESTER_RECORD_NOT_FOUND("A01", "해당 학기의 성적 데이터를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SEMESTER_RECORD_EMPTY("A02", "학기 성적 데이터를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FRESHMAN_NO_SEMESTER("A03", "신입생은 학기 기록이 없습니다.", HttpStatus.BAD_REQUEST),
    GRADUATION_REQUIREMENTS_NOT_FOUND("G01", "졸업 요건 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    // 학업 관련
    INVALID_GRADE_TYPE("A06", "존재하지 않는 성적 등급입니다.", HttpStatus.BAD_REQUEST),

    // 포털 관련
    PORTAL_LOGIN_FAILED("P01", "아이디나 비밀번호가 일치하지 않습니다.\n학교 홈페이지에서 확인해주세요.", HttpStatus.UNAUTHORIZED),
    PORTAL_SCRAPE_FAILED("P02", "포털 크롤링 실패", HttpStatus.INTERNAL_SERVER_ERROR),
    PORTAL_ACCOUNT_LOCKED("P03", "계정이 잠겼습니다. 포털사이트로 돌아가서 학번/사번 찾기 및 비밀번호 재발급을 진행해주세요", HttpStatus.LOCKED),

    // 공통(Common)
    INVALID_ARGUMENT("C01", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    NOT_FOUND("C05", "요청한 API를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 인증 및 세션 관련
    SESSION_EXPIRED("A04", "로그인 세션이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    // 인증 및 세션 관련
    AUTHENTICATION_REQUIRED("A05", "인증이 필요한 요청입니다.", HttpStatus.UNAUTHORIZED),

    // 서버 오류 관련
    SCRAPING_FAILED("C02", "포털 크롤링 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    REFRESH_FAILED("C03", "포털 정보 재연동 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FORBIDDEN("C04", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus status() {
        return status;
    }
}