package com.closetnangam.be;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
/*
 * CI는 application-test.yml의 MySQL 테스트 DB를 사용한다.
 * Spring Boot 테스트 자동 설정이 datasource를 embedded DB로 교체하지 않도록 명시한다.
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClosetnangamBeApplicationTests {

    @Test
    void contextLoads() {
    }

}
