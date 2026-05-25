package com.closetnangam.be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ClosetnangamBeApplication {
	public static void main(String[] args) {
		SpringApplication.run(ClosetnangamBeApplication.class, args);
	}

}
