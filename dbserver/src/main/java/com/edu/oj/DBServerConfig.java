package com.edu.oj;

import org.springframework.context.annotation.Configuration;
import org.mybatis.spring.annotation.MapperScan;

@Configuration
@MapperScan("com.edu.oj.mapper")
public class DBServerConfig {
    
}
