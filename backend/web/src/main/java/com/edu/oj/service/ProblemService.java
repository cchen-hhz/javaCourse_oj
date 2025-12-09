package com.edu.oj.service;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edu.oj.entity.Problem;
import com.edu.oj.exceptions.BusinessException;
import com.edu.oj.exceptions.CommonErrorCode;
import com.edu.oj.manager.FileSystemManager;
import com.edu.oj.mapper.ProblemMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProblemService {

    @Autowired
    private ProblemMapper problemMapper;

    @Autowired
    private FileSystemManager fileSystemManager;

    public Problem getProblemById(Long problemId) {
        Problem problem = problemMapper.findProblemById(problemId);
        if (problem == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "Problem not found: " + problemId);
        }
        return problem;
    }

    public Problem[] getAllProblems() {
        return problemMapper.getAllProblems();
    }

    public InputStream getProblemFile(Long problemId, String fileName) throws IOException {
        // Ensure problem exists
        getProblemById(problemId);
        return fileSystemManager.getProblemFileStream(problemId, fileName);
    }

    @Transactional
    public void uploadProblemData(Long problemId, InputStream dataStream) throws IOException {
        // Ensure problem exists
        getProblemById(problemId);
        fileSystemManager.saveAndUnzipProblemData(problemId, dataStream);
    }

    @Transactional
    public Problem createProblem(Long problemId, String title, InputStream zipFile) {
        Problem problem = new Problem();
        problem.setId(problemId);
        problem.setTitle(title);
        problemMapper.insertProblem(problem);
        
        try {
            fileSystemManager.saveAndUnzipProblemData(problem.getId(), zipFile);

            //检验数据完整性
            try {
                fileSystemManager.getProblemConfig(problem.getId());
            } catch (BusinessException e) {
                if (e.getErrorCode() == CommonErrorCode.RESOURCE_NOT_FOUND) {
                    throw new BusinessException(CommonErrorCode.BAD_REQUEST, "config.yml not found in the uploaded zip");
                }
                throw e;
            }
        } catch (Exception e) {
            log.error("Failed to init problem file system", e);
            throw new BusinessException(CommonErrorCode.FILE_OPERATION_ERROR, "Failed to init problem file system: " + e.getMessage());
        }
        
        return problem;
    }

    @Transactional
    public void deleteProblem(Long problemId) {
        // Ensure problem exists
        getProblemById(problemId);

        problemMapper.deleteProblemById(problemId);
        fileSystemManager.deleteProblemData(problemId);
    }
}
