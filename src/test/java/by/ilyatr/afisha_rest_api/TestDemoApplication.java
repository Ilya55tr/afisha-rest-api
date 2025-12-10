package by.ilyatr.afisha_rest_api;

import org.springframework.boot.SpringApplication;

public class TestDemoApplication {
    public static void main(String[] args) {
        SpringApplication.from(AfishaRestApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
