package spingcloud.diplom;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DiplomApplicationTests {

    @Test
    void contextLoads() {
        // Тест должен проходить с H2 базой
    }
}