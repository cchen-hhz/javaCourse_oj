package com.edu.oj.utils;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edu.oj.exceptions.BusinessException;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;

import com.edu.oj.judge.ProblemConfig;
import org.springframework.beans.factory.annotation.Qualifier;

@Component
@Slf4j
public class ProblemFileManager extends BaseFileManager {
    @Value("${data.problem-file-path}")
    private String problemFilePath;

    public ProblemFileManager(@Qualifier("yamlMapper") ObjectMapper yamlMapper) {
        super(yamlMapper);
    }

    @PostConstruct
    public void init() {
        if(problemFilePath == null || problemFilePath.isEmpty()) {
            throw new BusinessException("Problem file path is not configured");
        }
        log.info("Problem file path: " + problemFilePath);
    }
    
    private Path getProblemConfigPath(Long problemId) {
        Path path = Path.of(problemFilePath, problemId.toString());
        return path.resolve("config.yml");
    }

    /**
     * 获取题目根目录路径
     * @param problemId 题目ID
     * @return 路径
     */
    public Path getProblemPath(Long problemId) {
        return Path.of(problemFilePath, problemId.toString());
    }

    /**
     * 获取题目配置
     * @param problemId 题目ID
     * @return 题目配置
     * @throws BusinessException 题目不存在时抛出
     * @throws IOException 读取文件失败时抛出
     */
    public ProblemConfig getProblemConfig(Long problemId) throws IOException {
        Path configPath = getProblemConfigPath(problemId);
        return readConfig(configPath, ProblemConfig.class, "ProblemConfig", problemId);
    }   
}
