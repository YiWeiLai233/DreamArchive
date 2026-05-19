package com.yiweilai.DreamArchive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DreamArchiveApplication {

	public static void main(String[] args) {
		SpringApplication.run(DreamArchiveApplication.class, args);
	}

}
