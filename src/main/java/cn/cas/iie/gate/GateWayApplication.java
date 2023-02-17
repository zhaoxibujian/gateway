package cn.cas.iie.gate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class GateWayApplication {
    public static void main(String[] args){
        SpringApplication.run(GateWayApplication.class, args);
    }
}
