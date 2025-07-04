package com.driver.whatsapp.wrapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;





@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories("com.driver.whatsapp.wrapper.repository")
public class TruckDriverWhatsAppWrapperApplication {

	public static void main(String[] args) {
		SpringApplication.run(TruckDriverWhatsAppWrapperApplication.class, args);
	}

}
