package com.fitness.aiservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Starts the whole application context, which needs the config server, its databases and Kafka
 * running (see the README). Enabled with FITLIFE_STACK_TESTS=1; the other tests need none of it.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "FITLIFE_STACK_TESTS", matches = "1")
class AiserviceApplicationTests {

	@Test
	void contextLoads() {
	}

}
