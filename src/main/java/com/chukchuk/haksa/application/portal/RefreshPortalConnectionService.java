package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.service.DepartmentService;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import com.chukchuk.haksa.domain.user.model.StudentInitializationDataType;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserPortalConnectionRepository;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import com.chukchuk.haksa.infrastructure.portal.model.PortalStudentInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshPortalConnectionService {

    private final DepartmentService departmentService;
    private final UserPortalConnectionRepository userPortalConnectionRepository;
    private final UserService userService;

    @Transactional
    public PortalConnectionResult executeWithPortalData(UUID userId, PortalData portalData) {
        try {
            User user = userService.getUserById(userId);
            if (!user.getPortalConnected()) {
                return failure("아직 포털 계정과 연동되지 않은 사용자입니다.");
            }

            PortalStudentInfo raw = portalData.student();

            Department department = departmentService.getOrCreateDepartment(
                    raw.department().code(), raw.department().name());
            var majorDto = raw.major();
            Department major = (majorDto != null && majorDto.code() != null)
                    ? departmentService.getOrCreateDepartment(majorDto.code(), majorDto.name())
                    : null;
            Department secondaryMajor = raw.secondaryMajor() != null
                    ? departmentService.getOrCreateDepartment(raw.secondaryMajor().code(), raw.secondaryMajor().name())
                    : null;

            if (department == null) {
                log.error("[PORTAL][INIT] 학과 초기화 실패: userId={}, deptCode={}", userId, raw.department().code());
                return failure("학과/전공 정보 초기화 실패");
            }

            StudentInitializationDataType studentData = StudentInitializationDataType.builder()
                    .studentCode(raw.studentCode())
                    .name(raw.name())
                    .department(department)
                    .major(major)
                    .secondaryMajor(secondaryMajor)
                    .admissionYear(raw.admission().year())
                    .semesterEnrolled(raw.admission().semester())
                    .isTransferStudent(raw.admission().type().contains("편입"))
                    .isGraduated(raw.status().equals(StudentStatus.졸업.name()))
                    .status(StudentStatus.valueOf(raw.status()))
                    .gradeLevel(raw.academic().gradeLevel())
                    .completedSemesters(raw.academic().completedSemesters())
                    .admissionType(raw.admission().type())
                    .build();

            userPortalConnectionRepository.refreshPortalConnection(user, studentData);

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

        } catch (Exception e) {
            log.error("[PORTAL][INIT] 예외 발생: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("포털 연동 중 오류가 발생했습니다.", e);
        }
    }
}