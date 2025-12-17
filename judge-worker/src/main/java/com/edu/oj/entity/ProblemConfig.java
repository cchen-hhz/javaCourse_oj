package com.edu.oj.entity;

import lombok.Data;

@Data
public class ProblemConfig {
    private Long id;
    private String title;
    private Long number_count;
    private Long time_limit;
    private Long memory_limit;
}
