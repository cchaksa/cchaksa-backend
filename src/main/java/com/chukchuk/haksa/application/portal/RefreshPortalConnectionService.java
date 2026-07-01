package com.chukchuk.haksa.application.portal;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshPortalConnectionService {

    private final UserPortalConnectionRepository userPortalConnectionRepository;
    private final UserService userService;
    private final PortalStudentDataMapper portalStudentDataMapper;

    @Transactional
    public PortalConnectionResult executeWithPortalData(UUID userId, PortalData portalData) {

        try {
            User user = userService.getUserById(userId);

            if (!user.getPortalConnected()) {
                // 정상 흐름 X -> WARN
                log.warn("[BIZ] portal.conn.fail userId={} reason=not_connected", userId);
                return failure("아직 포털 계정과 연동되지 않은 사용자입니다.");
            }

            if (portalData == null || portalData.student() == null) {
                log.warn("[BIZ] portal.conn.fail userId={} reason=portal_data_null", userId);
                return failure("포털 데이터가 존재하지 않습니다.");
            }

            PortalStudentInfo raw = portalData.student();
            PortalStudentDataMapper.PortalStudentData portalStudentData =
                    portalStudentDataMapper.toStudentData(raw);
            if (portalStudentData == null) {
                log.warn("[BIZ] portal.conn.fail userId={} reason=student_data_mapping_failed", userId);
                return failure("포털 학생 정보 초기화 실패");
            }

            userPortalConnectionRepository.refreshPortalConnection(user, portalStudentData.studentData());

            return success(raw.studentCode(), portalStudentData.studentInfo());

        } catch (Exception e) {
            log.warn("[BIZ] portal.conn.ex userId={} ex={}", userId, e.getClass().getSimpleName(), e);
            throw new RuntimeException("포털 연동 중 오류가 발생했습니다.", e);
        }
    }
}
