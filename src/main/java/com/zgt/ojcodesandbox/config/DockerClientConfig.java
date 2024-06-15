package com.zgt.ojcodesandbox.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author : JAT
 * @version : 1.0
 * @email : zgt9321@qq.com
 * @since : 2024/6/15
 **/
@Configuration
public class DockerClientConfig {
    @Bean
    public DockerClient getDockerClient() {
        return DockerClientBuilder.getInstance().build();
    }
}
