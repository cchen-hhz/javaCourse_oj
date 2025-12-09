package com.edu.oj.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private static final String JUDGE_TOPIC = "judger";
    private static final String RESULT_TOPIC = "judge-result";

    @Autowired
    FileSystemManager fileManager;

    @Autowired
    SubmissionMapper submissionMapper;

    @Autowired
    KafkaTemplate<String, SubmissionMessage> kafkaTemplate;

    ConcurrentMap<Long, SubmissionConfig> submissionCache = new ConcurrentHashMap<>();

    @Transactional
    public void handleSubmission(SubmissionDto submissionRequest) throws IOException{
        Submission sub = new Submission(
            null,
            LocalDateTime.now(),
            Status.PENDING,
            submissionRequest.getUserId(),
            submissionRequest.getProblemId(),
            submissionRequest.getLanguage(),
            null
        );
        submissionMapper.insertSubmission(sub);
        fileManager.saveSubmissionCode(sub.getId(), submissionRequest.getCode());

        //TODO: kafa
    }
    
    public void sendSubmission(Submission submission) {
        log.info("Sending submission to judge queue:" + submission.getId());
        kafkaTemplate.send(JUDGE_TOPIC, new SubmissionMessage(submission.getId(), submission.getProblemId()));
    }

    @Transactional
    @KafkaListener(topics = RESULT_TOPIC, groupId = "judge-service-group")
    public void receiveJudgeResult(ResultMessage message) {
        log.info("Received judge result for submission: {} for case {}", message.getSubmissionId(), message.getTestcase());
        
        SubmissionConfig config = submissionCache.computeIfAbsent(message.getSubmissionId(), k -> {
            SubmissionConfig c = new SubmissionConfig();
            c.setTestResult(new ArrayList<>());
            c.setTimeUsed(0);
            c.setMemoryUsed(0);
            return c;
        });

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