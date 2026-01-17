package com.example.visited;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
@SpringBootApplication
public class MarketingSchoolsVisitedApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarketingSchoolsVisitedApplication.class, args);
	}
	@Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // optional: strength 12 is good balance
    }
}
