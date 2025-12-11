package com.edu.oj.executor.domain;

import lombok.Data;

import java.nio.file.Path;

@Data
public class CompileResult {
    private final boolean success;
    private final String message;
    private final Path executablePath;
}