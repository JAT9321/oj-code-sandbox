package com.zgt.ojcodesandbox;

import com.zgt.ojcodesandbox.service.DockerService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OjCodeSandboxApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(OjCodeSandboxApplication.class, args);
        // DockerService dockerService = (DockerService) applicationContext.getBean("dockerServiceImpl");
        // System.out.println(dockerService);

    }

}
