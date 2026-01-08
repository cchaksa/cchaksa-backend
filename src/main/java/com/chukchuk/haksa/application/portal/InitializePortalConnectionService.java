package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.service.DepartmentService;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.domain.user.model.StudentInitializationDataType;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserPortalConnectionRepository;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.CommonException;
import com.chukchuk.haksa.global.exception.ErrorCode;
import com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import com.chukchuk.haksa.infrastructure.portal.model.PortalStudentInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult.*;

/* 포털 연동 초기화 유스케이스 실행 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InitializePortalConnectionService {

    private final DepartmentService departmentService;
    private final UserPortalConnectionRepository userPortalConnectionRepository;
    private final UserService userService;
    private final StudentService studentService;

    @Transactional
    public PortalConnectionResult executeWithPortalData(UUID userId, PortalData portalData) {
        try {
            User user = userService.getUserById(userId);
            // TODO: Controller 단에 동일한 로직 존재 -> 제거 예정
            if (user.getPortalConnected()) {
                return failure("이미 포털 계정과 연동된 사용자입니다.");
            }

            PortalStudentInfo raw = portalData.student();

            // 학과 및 전공 정보 설정
            Department department = departmentService.getOrCreateDepartment(
                    raw.department().code(), raw.department().name());

            var majorDto = raw.major();
            Department major = (majorDto != null && majorDto.code() != null)
                    ? departmentService.getOrCreateDepartment(majorDto.code(), majorDto.name())
                    : null;

            Department secondaryMajor = raw.secondaryMajor() != null
                    ? departmentService.getOrCreateDepartment(
                    raw.secondaryMajor().code(), raw.secondaryMajor().name())
                    : null;

            // StudentInitializationDataType 생성
            if (department == null) {
                throw new IllegalStateException("학과/전공 정보 초기화 실패");
            }

            String studentCode = raw.studentCode();

            if (studentService.existsByStudentCode(studentCode)) {
                throw new CommonException(ErrorCode.DUPLICATED_STUDENT_CODE);
            }

            // 학과 및 전공 정보 포함된 StudentInitializationDataType 생성
            StudentInitializationDataType studentData = StudentInitializationDataType.builder()
                    .studentCode(raw.studentCode())
                    .name(raw.name())
                    .department(department)  // 학과 객체
                    .major(major)            // 전공 객체
                    .secondaryMajor(secondaryMajor) // 복수 전공 객체
                    .admissionYear(raw.admission().year())
                    .semesterEnrolled(raw.admission().semester())
                    .isTransferStudent(raw.admission().type().contains("편입"))
                    .isGraduated(raw.status().equals(StudentStatus.졸업.name()))
                    .status(StudentStatus.valueOf(raw.status()))
                    .gradeLevel(raw.academic().gradeLevel())
                    .completedSemesters(raw.academic().completedSemesters())
                    .admissionType(raw.admission().type())
                    .build();


            // 포털 연동 초기화
            userPortalConnectionRepository.initializePortalConnection(user, studentData);

            StudentInfo studentInfo = new StudentInfo(
                    raw.name(),
                    "수원대학교",
                    major != null ? major.getEstablishedDepartmentName() : department.getEstablishedDepartmentName(),
                    raw.studentCode(),
                    raw.academic().gradeLevel(),
                    raw.status(),
                    raw.academic().completedSemesters() % 2 == 0 ? 1 : 2
            );

            return success(raw.studentCode(), studentInfo);
        } catch (DataIntegrityViolationException e) {
            if (isStudentCodeUniqueViolation(e)) {
                throw new CommonException(ErrorCode.DUPLICATED_STUDENT_CODE);
            }
            throw e;
        } catch (Exception e) {
            log.error("[PORTAL][INIT] 예외 발생: {}", e.getMessage(), e);
            throw new CommonException(ErrorCode.SCRAPING_FAILED);
        }
    }

    private boolean isStudentCodeUniqueViolation(DataIntegrityViolationException e) {
        Throwable cause = e.getMostSpecificCause();
        return cause != null && cause.getMessage().contains("student_code");
    }
}