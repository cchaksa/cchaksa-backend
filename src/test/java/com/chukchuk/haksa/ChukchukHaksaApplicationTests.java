package com.chukchuk.haksa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("로컬 PostgreSQL/.env 의존으로 CI 및 독립 실행 테스트 환경에서 컨텍스트 로드가 불가함")
class ChukchukHaksaApplicationTests {

	@Test
	void contextLoads() {
	}

}
