package it.unipi.riskDeV;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableScheduling
public class RiskDeVApplication {

	public static void main(String[] args) {
		clearAppLog();
		SpringApplication.run(RiskDeVApplication.class, args);
	}

	private static void clearAppLog() {
		Path logPath = Paths.get("logs/app.log");
		try {
			if (Files.exists(logPath)) {
				Files.newBufferedWriter(logPath).close();
			} else {
				System.out.println("logs/app.log does not exist, nothing to clear.");
			}
		} catch (IOException e) {
			e.printStackTrace();
			}
	}

}
