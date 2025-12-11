package com.edu.oj.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import com.edu.oj.dto.SubmissionDto;
import com.edu.oj.entity.Status;
import com.edu.oj.entity.Submission;
import com.edu.oj.manager.FileSystemManager;
import com.edu.oj.mapper.SubmissionMapper;
import com.edu.oj.message.SubmissionMessage;
import com.edu.oj.message.ResultMessage;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import com.edu.oj.entity.SubmissionConfig;
import com.edu.oj.entity.TestResult;
import com.edu.oj.exceptions.BusinessException;
import com.edu.oj.exceptions.CommonErrorCode;

@Service
@Slf4j
public class JudgeService {
    @Value("${spring.kafka.topic.submission}")
    private String judgeTopic;

    @Autowired
    FileSystemManager fileManager;

    @Autowired
    SubmissionMapper submissionMapper;

    @Autowired
    KafkaTemplate<String, SubmissionMessage> kafkaTemplate;

    ConcurrentMap<Long, SubmissionConfig> submissionCache = new ConcurrentHashMap<>();

    @Transactional
    public Long handleSubmission(Long userId, SubmissionDto submissionRequest) throws IOException{
        Submission sub = new Submission(
            null,
            LocalDateTime.now(),
            Status.PENDING,
            userId,
            submissionRequest.getProblemId(),
            submissionRequest.getLanguage(),
            null
        );
        submissionMapper.insertSubmission(sub);
        fileManager.saveSubmissionCode(sub.getId(), submissionRequest.getCode(), submissionRequest.getLanguage());

        sendSubmission(sub);
        return sub.getId();
    }
    
    @SuppressWarnings("null")
    public void sendSubmission(Submission submission) {
        log.info("Sending submission to judge queue:" + submission.getId());
        kafkaTemplate.send(judgeTopic, new SubmissionMessage(submission.getId(), submission.getProblemId(), submission.getLanguage()));
    }

    @Transactional
    @KafkaListener(topics = "${spring.kafka.topic.result}", groupId = "judge-service-group")
    public void receiveJudgeResult(ResultMessage message) {
        log.info("Received judge result for submission: {} for case {}", message.getSubmissionId(), message.getTestcase());
        
        SubmissionConfig config = submissionCache.computeIfAbsent(message.getSubmissionId(), k -> {
            SubmissionConfig c = new SubmissionConfig();
            c.setTestResult(new ArrayList<>());
            c.setTimeUsed(0);
            c.setMemoryUsed(0);
            return c;
        });

        synchronized (config) {
            config.setStatus(message.getStatus());

            if (message.getTestcase() == 0) {
                config.setCompileMessage(message.getMessage());
            } else {
                TestResult testResult = new TestResult();
                testResult.setCaseId(message.getTestcase());
                testResult.setStatus(message.getStatus());
                testResult.setTime(message.getTimeUsed());
                testResult.setMemory(message.getMemoryUsed());
                testResult.setInput(message.getInput());
                testResult.setUserOutput(message.getUserOutput());
                testResult.setExpectedOutput(message.getExpectedOutput());
                testResult.setMessage(message.getMessage());
                config.getTestResult().add(testResult);

                config.setTimeUsed(Math.max(config.getTimeUsed(), message.getTimeUsed()));
                config.setMemoryUsed(Math.max(config.getMemoryUsed(), message.getMemoryUsed()));
            }

            if (Boolean.TRUE.equals(message.getComplete())) {
                try {
                    fileManager.saveSubmissionConfig(message.getSubmissionId(), config);
                    submissionMapper.updateSubmissionStatusById(message.getSubmissionId(), Status.DONE);
                    submissionCache.remove(message.getSubmissionId());
                } catch (IOException e) {
                    throw new BusinessException(CommonErrorCode.FILE_OPERATION_ERROR, "Failed to save submission config for submissionId: " + message.getSubmissionId());
                }
            }
        }
    }

    public SubmissionConfig getSubmissionResult(Long submissionId) throws IOException {
        Submission submission = submissionMapper.findSubmissionById(submissionId);
        if (submission == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "Submission not found");
        }

        if (submission.getStatus() == Status.PENDING || submission.getStatus() == Status.JUDGING) {
            SubmissionConfig config = submissionCache.get(submissionId);
            if (config != null) {
                return config;
            }
            
            // 5 分钟后自动寄寄
            if (submission.getSubmissionTime().plusMinutes(5).isBefore(LocalDateTime.now())) {
                log.warn("Submission {} timed out", submissionId);
                SubmissionConfig errorConfig = new SubmissionConfig();
                errorConfig.setStatus(6); // SYSTEM_ERROR
                errorConfig.setTestResult(new ArrayList<>());
                
                fileManager.saveSubmissionConfig(submissionId, errorConfig);
                submissionMapper.updateSubmissionStatusById(submissionId, Status.DONE);
                
                return errorConfig;
            }
            
            return new SubmissionConfig();
        } else {
            return fileManager.getSubmissionConfig(submissionId);
        }
    }

    public String getSubmissionCode(Long submissionId) throws IOException {
        Submission submission = submissionMapper.findSubmissionById(submissionId);
        if (submission == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "Submission not found");
        }
        String extension = FileSystemManager.getExtensionByLanguage(submission.getLanguage());
        try (java.io.InputStream inputStream = fileManager.getSubmissionFileStream(submissionId, "code." + extension)) {
            return new String(inputStream.readAllBytes());
        }
    }

    public Submission getSubmissionById(Long submissionId) {
        return submissionMapper.findSubmissionById(submissionId);
    }
}