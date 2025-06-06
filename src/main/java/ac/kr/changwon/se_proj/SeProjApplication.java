package ac.kr.changwon.se_proj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "ac.kr.changwon.se_proj" )
@EnableScheduling
public class SeProjApplication {

    public static void main(String[] args) {

        SpringApplication.run(SeProjApplication.class, args);

    }

}
