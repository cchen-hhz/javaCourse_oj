package com.edu.oj.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;  
    private String description; //用户自定义描述
    private Role role; 
    private LocalDateTime createdAt;
    private Boolean enabled;
}
