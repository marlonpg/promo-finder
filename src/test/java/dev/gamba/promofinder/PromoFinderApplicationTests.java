package dev.gamba.promofinder;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:test",
		"promo.scheduler.tick=1h" })
class PromoFinderApplicationTests {

	@Test
	void contextLoads() {
	}

}
