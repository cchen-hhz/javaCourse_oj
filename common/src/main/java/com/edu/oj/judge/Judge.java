package com.edu.oj.judge;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Judge {
    public Integer number;
    public Integer timeLimit;
    public Integer memoryLimit;
    public List<String> testcases;
}