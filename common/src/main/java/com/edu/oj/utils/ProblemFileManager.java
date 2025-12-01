package com.edu.oj.utils;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import com.edu.oj.judge.ProblemConfig;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * 用于题目文件的存储，加载管理 
 */
@Component
@Slf4j
public class ProblemFileManager {
    @Value("${data.problem-file-path}")
    private String problemFilePath;

    @PostConstruct
    public void init() {
        log.info("Problem file path: " + problemFilePath);
    }

    @Autowired
    ObjectMapper yamlMapper;

    private Path getProblemPath(Long problemId) {
        return Path.of(problemFilePath, problemId.toString());
    }

    private Path getProblemConfigPath(Long problemId) {
        return getProblemPath(problemId).resolve("config.yml");
    }

    public ProblemConfig getProblemConfig(Long problemId) throws Exception {
        Path configPath = getProblemConfigPath(problemId);
        if(!configPath.toFile().exists()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Problem not found: " + problemId 
            );
        }
        return yamlMapper.readValue(configPath.toFile(), ProblemConfig.class);
    }   

    public int writeProblemConfig(Long problemId, ProblemConfig config) throws IOException {
        Path configPath = getProblemConfigPath(problemId);
        yamlMapper.writeValue(configPath.toFile(), config);
        return 0;
    }
}
