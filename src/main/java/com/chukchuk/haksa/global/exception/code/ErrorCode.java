package com.chukchuk.haksa.global.exception.code;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 공통(Common)
    INVALID_ARGUMENT("C01", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    NOT_FOUND("C05", "요청한 API를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SCRAPE_JOB_NOT_FOUND("C06", "스크래핑 작업을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    IDEMPOTENCY_KEY_CONFLICT("C07", "동일한 Idempotency-Key로 다른 요청을 보낼 수 없습니다.", HttpStatus.CONFLICT),
    INVALID_CALLBACK_SIGNATURE("C08", "유효하지 않은 내부 콜백 서명입니다.", HttpStatus.UNAUTHORIZED),
    UNSUPPORTED_PORTAL_TYPE("C09", "지원하지 않는 포털 타입입니다.", HttpStatus.BAD_REQUEST),
    SCRAPE_JOB_ENQUEUE_FAILED("C10", "스크래핑 작업 큐 등록에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    CALLBACK_TIMEOUT("C11", "스크래핑 결과 콜백이 시간 내 도착하지 않았습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    SCRAPE_JOB_OUTBOX_DEAD("C12", "스크래핑 작업 전송이 중단되었습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    SCRAPE_JOB_NOT_COMPLETED("C13", "스크래핑 작업이 아직 완료되지 않았습니다.", HttpStatus.CONFLICT),
    SCRAPE_JOB_FAILED_RESULT("C14", "스크래핑 작업이 실패 상태입니다.", HttpStatus.UNPROCESSABLE_ENTITY),
    SCRAPE_INVALID_S3_KEY("C15", "허용되지 않은 S3 key 입니다.", HttpStatus.BAD_REQUEST),
    SCRAPE_RESULT_S3_FAILED("C16", "S3에서 스크래핑 결과를 읽어오지 못했습니다.", HttpStatus.BAD_GATEWAY),
    SCRAPE_RESULT_SCHEMA_INVALID("C17", "스크래핑 결과 스키마가 유효하지 않습니다.", HttpStatus.UNPROCESSABLE_ENTITY),
    SCRAPE_RESULT_POST_PROCESSING_FAILED("C18", "스크래핑 결과 후처리에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    SCRAPE_INVALID_CALLBACK_REQUEST("C19", "스크래핑 콜백 요청이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    SCRAPE_RESULT_S3_ACCESS_DENIED("C20", "S3에서 스크래핑 결과를 읽을 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // 인증 및 세션 관련
    SESSION_EXPIRED("A04", "로그인 세션이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    // 인증 및 세션 관련
    AUTHENTICATION_REQUIRED("A05", "인증이 필요한 요청입니다.", HttpStatus.UNAUTHORIZED),

    // 서버 오류 관련
    SCRAPING_FAILED("C02", "포털 크롤링 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    REFRESH_FAILED("C03", "포털 정보 재연동 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FORBIDDEN("C04", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

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
    USER_ALREADY_CONNECTED("U03", "이미 포털과 연동된 사용자입니다.", HttpStatus.BAD_REQUEST),
    USER_NOT_CONNECTED("U04", "아직 포털과 연동되지 않은 사용자입니다.", HttpStatus.BAD_REQUEST),

    // Student 관련
    STUDENT_NOT_FOUND("S01", "해당 학생이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    INVALID_TARGET_GPA("S02", "유효하지 않은 목표 학점입니다.", HttpStatus.BAD_REQUEST),
    STUDENT_ID_REQUIRED("S03", "Student ID는 필수입니다.", HttpStatus.BAD_REQUEST),
    TRANSFER_STUDENT_UNSUPPORTED("T13", "편입생 학적 정보는 현재 지원되지 않습니다.", HttpStatus.UNPROCESSABLE_ENTITY),

    // 학업 관련
    SEMESTER_RECORD_NOT_FOUND("A01", "해당 학기의 성적 데이터를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SEMESTER_RECORD_EMPTY("A02", "학기 성적 데이터를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FRESHMAN_NO_SEMESTER("A03", "신입생은 학기 기록이 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_GRADE_TYPE("A06", "존재하지 않는 성적 등급입니다.", HttpStatus.BAD_REQUEST),

    // 졸업 요건 관련
    GRADUATION_REQUIREMENTS_DATA_NOT_FOUND("G02", "사용자에게 맞는 졸업 요건 데이터가 존재하지 않습니다.", HttpStatus.NOT_FOUND),

    // 포털 관련
    PORTAL_LOGIN_FAILED("P01", "아이디나 비밀번호가 일치하지 않습니다.\n학교 홈페이지에서 확인해주세요.", HttpStatus.UNAUTHORIZED),
    PORTAL_SCRAPE_FAILED("P02", "포털 크롤링 실패", HttpStatus.INTERNAL_SERVER_ERROR),
    PORTAL_ACCOUNT_LOCKED("P03", "계정이 잠겼습니다. 포털사이트로 돌아가서 학번/사번 찾기 및 비밀번호 재발급을 진행해주세요", HttpStatus.LOCKED);

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
