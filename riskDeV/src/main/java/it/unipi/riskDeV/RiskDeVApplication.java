package it.unipi.riskDeV;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RiskDeVApplication {

	public static void main(String[] args) {
		SpringApplication.run(RiskDeVApplication.class, args);
	}

}
