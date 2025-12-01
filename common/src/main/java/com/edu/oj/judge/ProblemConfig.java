package com.edu.oj.judge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProblemConfig {
    public Long id;
    public String title;
    public Judge judge; 
}
