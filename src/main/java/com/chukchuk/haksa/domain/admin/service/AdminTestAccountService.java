// dev 테스트 계정 생성과 토큰 발급을 처리한다
package com.chukchuk.haksa.domain.admin.service;

import com.chukchuk.haksa.domain.admin.dto.AdminTestDto;
import com.chukchuk.haksa.domain.auth.dto.AuthDto;
import com.chukchuk.haksa.domain.auth.service.RefreshTokenService;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.security.service.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminTestAccountService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter SUFFIX_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    public AdminTestDto.TestUserResponse createTestUser(AdminTestDto.CreateTestUserRequest request) {
        String suffix = createSuffix();
        String email = "test_" + suffix + "@cchaksa.dev";
        String studentCode = "test_" + suffix;
        String name = request.name() == null || request.name().isBlank() ? "프론트테스트" : request.name().trim();
        Department department = resolveDepartment(request.departmentId());
        Department major = request.majorId() != null ? resolveDepartment(request.majorId()) : department;
        Department secondaryMajor = request.secondaryMajorDepartmentId() != null
                ? resolveDepartment(request.secondaryMajorDepartmentId())
                : null;

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .profileNickname(name)
                .build();
        if (!Boolean.FALSE.equals(request.isPortalLinked())) {
            user.markPortalConnected(Instant.now());
        }
        User savedUser = userRepository.save(user);

        Student student = Student.builder()
                .studentCode(studentCode)
                .name(name)
                .department(department)
                .major(major)
                .secondaryMajor(secondaryMajor)
                .admissionYear(request.admissionYear() != null ? request.admissionYear() : LocalDateTime.now(SEOUL).getYear())
                .semesterEnrolled(1)
                .isTransferStudent(false)
                .isGraduated(false)
                .status(StudentStatus.재학)
                .gradeLevel(1)
                .completedSemesters(0)
                .admissionType("신입")
                .user(savedUser)
                .build();
        Student savedStudent = studentRepository.save(student);
        savedUser.setStudent(savedStudent);

        String userId = savedUser.getId().toString();
        String accessToken = jwtProvider.createAccessToken(userId, savedUser.getEmail(), "USER");
        AuthDto.RefreshTokenWithExpiry refreshToken = jwtProvider.createRefreshToken(userId);
        refreshTokenService.save(refreshToken.sessionId(), userId, refreshToken.token(), refreshToken.expiry());

        return new AdminTestDto.TestUserResponse(
                savedUser.getId(),
                savedStudent.getId(),
                savedUser.getEmail(),
                savedStudent.getStudentCode(),
                accessToken,
                refreshToken.token()
        );
    }

    private Department resolveDepartment(Long departmentId) {
        if (departmentId != null) {
            return departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new CommonException(ErrorCode.INVALID_ARGUMENT));
        }

        return departmentRepository.findAll(PageRequest.of(0, 1)).stream()
                .findFirst()
                .orElseGet(() -> departmentRepository.save(new Department("test_department", "테스트학과")));
    }

    private String createSuffix() {
        return LocalDateTime.now(SEOUL).format(SUFFIX_FORMAT)
                + "_"
                + UUID.randomUUID().toString().substring(0, 8);
    }
}
