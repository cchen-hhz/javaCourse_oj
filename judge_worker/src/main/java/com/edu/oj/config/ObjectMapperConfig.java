package com.edu.oj.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectMapperConfig {

    @Bean(name="yamlMapper")
    public ObjectMapper yamlMapper() {
        return new ObjectMapper(new YAMLFactory());
    }

    @Bean(name="jsonMapper")
    public ObjectMapper jsonMapper() {
        return new ObjectMapper();
    }
}
