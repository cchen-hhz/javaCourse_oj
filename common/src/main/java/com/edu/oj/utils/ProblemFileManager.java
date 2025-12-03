package com.edu.oj.utils;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edu.oj.exceptions.ResourceNotFoundException;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.edu.oj.judge.ProblemConfig;
import org.springframework.beans.factory.annotation.Autowired;

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
    
    private Path getProblemConfigPath(Long problemId) {
        Path path = Path.of(problemFilePath, problemId.toString());
        return path.resolve("config.yml");
    }

    /**
     * 获取题目配置
     * @param problemId 题目ID
     * @return 题目配置
     * @throws ResourceNotFoundException 题目不存在时抛出
     * @throws IOException 读取文件失败时抛出
     */
    public ProblemConfig getProblemConfig(Long problemId) throws IOException {
        Path configPath = getProblemConfigPath(problemId);
        if(!Files.exists(configPath)) {
            throw new ResourceNotFoundException("ProblemConfig", problemId);
        }
        return yamlMapper.readValue(configPath.toFile(), ProblemConfig.class);
    }   

    /**
     * 写入题目配置
     * 使用原子操作（写临时文件 -> 移动），防止在写入过程中被读取导致数据不完整
     * @param problemId 题目ID
     * @param config 配置对象
     * @return 0
     * @throws IOException 写入失败
     */
    public int writeProblemConfig(Long problemId, ProblemConfig config) throws IOException {
        Path configPath = getProblemConfigPath(problemId);
        Path parentDir = configPath.getParent();
        
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        Path tempFile = Files.createTempFile(parentDir, "config-", ".tmp");
        try {
            yamlMapper.writeValue(tempFile.toFile(), config);
            Files.move(tempFile, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
            throw e;
        }
        return 0;
    }
}
