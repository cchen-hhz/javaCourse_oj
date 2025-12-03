package com.edu.oj.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Path;

import com.edu.oj.judge.SubmissionConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SubmissionFileManager {
    @Value("${data.submission-file-path}")
    private String submissionFilePath;

    @Autowired
    ObjectMapper jsonMapper;

    private Path getSubmissionResultPath(Long submissionId) {
        Path path = Path.of(submissionFilePath, submissionId.toString());
        return path.resolve("result.json");
    }

    public SubmissionConfig getSubmissionConfig(Long submissionId) throws Exception {
        Path resultPath = getSubmissionResultPath(submissionId);
        if (resultPath.toFile().exists() == false) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Submission Not found: " + submissionId
            );
        }
        return jsonMapper.readValue(resultPath.toFile(), SubmissionConfig.class);
    }
}
