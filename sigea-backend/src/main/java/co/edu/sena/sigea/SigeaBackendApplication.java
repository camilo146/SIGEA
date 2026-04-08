package co.edu.sena.sigea;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SigeaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SigeaBackendApplication.class, args);
	}

}
