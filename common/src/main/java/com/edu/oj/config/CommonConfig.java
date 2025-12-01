package com.edu.oj.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Configuration
public class CommonConfig {
    @Bean("yamlMapper")
    @Primary
    ObjectMapper yamlMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return mapper;
    }

    @Bean("jsonMapper")
    
    ObjectMapper jsonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper;
    }
}
