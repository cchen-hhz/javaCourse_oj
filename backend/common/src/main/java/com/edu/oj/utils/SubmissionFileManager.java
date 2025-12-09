package com.edu.oj.utils;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.file.Path;
import java.io.IOException;

import com.edu.oj.entity.SubmissionConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import com.edu.oj.exceptions.BusinessException;

@Component
@Slf4j
public class SubmissionFileManager extends BaseFileManager {
    @Value("${data.submission-file-path}")
    private String submissionFilePath;

    public SubmissionFileManager(@Qualifier("jsonMapper") ObjectMapper jsonMapper) {
        super(jsonMapper);
    }

    @PostConstruct
    public void init() {
        if(submissionFilePath == null || submissionFilePath.isEmpty()) {
            throw new BusinessException("Submission file path is not configured");
        }
        log.info("Submission file path: " + submissionFilePath);
    }

    private Path getSubmissionResultPath(Long submissionId) {
        Path path = Path.of(submissionFilePath, submissionId.toString());
        return path.resolve("result.json");
    }

    /**
     * 获取提交根目录路径
     * @param submissionId 提交ID
     * @return 路径
     */
    public Path getSubmissionPath(Long submissionId) {
        return Path.of(submissionFilePath, submissionId.toString());
    }

    public SubmissionConfig getSubmissionConfig(Long submissionId) throws IOException {
        Path resultPath = getSubmissionResultPath(submissionId);
        return readConfig(resultPath, SubmissionConfig.class, "Submission", submissionId);
    }

    public void saveSubmissionConfig(Long submissionId, SubmissionConfig config) throws IOException {
        Path resultPath = getSubmissionResultPath(submissionId);
        writeConfig(resultPath, config);
    }
}
