package by.ilyatr.afisha_rest_api.unit;

import by.ilyatr.afisha_rest_api.configuration.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;


@Testcontainers
@Import(TestcontainersConfiguration.class)
class AfishaRestApiApplicationTests {

	@Test
	void contextLoads() {}
}
