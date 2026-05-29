package com.closetnangam.be;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        // H2는 MySQL dialect 없이 동작하므로 JPA dialect 자동 감지에 맡김
        "spring.jpa.properties.hibernate.dialect="
})
class ClosetnangamBeApplicationTests {

    @Test
    void contextLoads() {
    }

}
