package com.edu.oj.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProblemConfig {
    public Long id;
    public String title;
    public Integer numberCount;
    public Integer timeLimit;
    public Integer memoryLimit;
}
