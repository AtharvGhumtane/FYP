package fyp.example.classroom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableFeignClients
@EnableAsync
@ComponentScan(basePackages = {"fyp.example.classroom", "fyp.example.security"})
public class ClassroomServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClassroomServiceApplication.class, args);
    }
}
