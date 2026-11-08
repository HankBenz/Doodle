package dev.hbn.doodle;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.docker.compose.skip.in-tests=false")
class DoodleApplicationTests {

	@Test
	void contextLoads() {
	}

}
