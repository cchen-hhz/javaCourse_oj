package com.edu.oj.manager;

import com.edu.oj.exceptions.BusinessException;
import com.edu.oj.exceptions.CommonErrorCode;
import com.edu.oj.judge.ProblemConfig;
import com.edu.oj.utils.ProblemFileManager;
import com.edu.oj.utils.SubmissionFileManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 文件系统管理器
 * 整合 ProblemFileManager 和 SubmissionFileManager，提供统一的文件读写接口
 */
@Component
@Slf4j
public class FileSystemManager {

    @Autowired
    private ProblemFileManager problemFileManager;

    @Autowired
    private SubmissionFileManager submissionFileManager;

    /**
     * 保存题目压缩包并解压
     * @param problemId 题目ID
     * @param zipStream 压缩文件流
     * @throws IOException IO异常
     */
    public void saveAndUnzipProblemData(Long problemId, InputStream zipStream) throws IOException {
        Path targetDir = problemFileManager.getProblemPath(problemId);
        Path parentDir = targetDir.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        // 创建临时目录
        Path tempDir = Files.createTempDirectory(parentDir, "temp_problem_" + problemId + "_");

        try {
            // 解压逻辑到临时目录
            try (ZipInputStream zis = new ZipInputStream(zipStream)) {
                ZipEntry zipEntry = zis.getNextEntry();
                while (zipEntry != null) {
                    Path newPath = tempDir.resolve(zipEntry.getName()).normalize(); 
                    
                    if (!newPath.startsWith(tempDir)) {
                        throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
                    }

                    if (zipEntry.isDirectory()) {
                        Files.createDirectories(newPath);
                    } else {
                        if (newPath.getParent() != null && !Files.exists(newPath.getParent())) {
                            Files.createDirectories(newPath.getParent());
                        }
                        Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zipEntry = zis.getNextEntry();
                }
            }

            // 解压成功，替换原有目录
            if (Files.exists(targetDir)) {
                FileSystemUtils.deleteRecursively(targetDir);
            }
            
            try {
                Files.move(tempDir, targetDir, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tempDir, targetDir, StandardCopyOption.REPLACE_EXISTING);
            }
            
            log.info("Problem data unzipped for problemId: {}", problemId);
        } catch (IOException e) {
            // 发生异常，清理临时目录
            try {
                FileSystemUtils.deleteRecursively(tempDir);
            } catch (IOException ex) {
                log.error("Failed to clean up temp dir: " + tempDir, ex);
            }
            throw e;
        }
    }

    /**
     * 保存提交的代码文件
     * @param submissionId 提交ID
     * @param code 代码内容
     * @throws IOException IO异常
     */
    public void saveSubmissionCode(Long submissionId, String code) throws IOException {
        Path dir = submissionFileManager.getSubmissionPath(submissionId);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        //TODO: 目前默认 cpp，未来需要拓展
        Path file = dir.resolve("code.cpp");
        Files.writeString(file, code);
        log.info("Submission code saved for submissionId: {}", submissionId);
    }

    /**
     * 读取题目文件流
     * @param problemId 题目ID
     * @param relativePath 相对路径
     * @return 文件输入流
     * @throws IOException IO异常   
     */
    public InputStream getProblemFileStream(Long problemId, String relativePath) throws IOException {
        Path root = problemFileManager.getProblemPath(problemId);
        Path file = root.resolve(relativePath).normalize();
        
        if (!Files.exists(file)) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "File not found: " + relativePath);
        }

        if (!Files.isRegularFile(file)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "Not a regular file: " + relativePath);
        }
        
        return Files.newInputStream(file);
    }

    /**
     * 读取提交文件流
     * @param submissionId 提交ID
     * @param relativePath 相对路径
     * @return 文件输入流
     * @throws IOException IO异常
     */
    public InputStream getSubmissionFileStream(Long submissionId, String relativePath) throws IOException {
        Path root = submissionFileManager.getSubmissionPath(submissionId);
        Path file = root.resolve(relativePath).normalize();

        if (!Files.exists(file)) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "File not found: " + relativePath);
        }

        return Files.newInputStream(file);
    }

    public ProblemConfig getProblemConfig(Long problemId) throws IOException {
        return problemFileManager.getProblemConfig(problemId);
    }

    /**
     * 删除题目数据
     * @param problemId 题目ID
     */
    public void deleteProblemData(Long problemId) {
        Path targetDir = problemFileManager.getProblemPath(problemId);
        try {
            FileSystemUtils.deleteRecursively(targetDir);
        } catch (Exception e) {
            log.error("Failed to delete problem data for problemId: " + problemId, e);
        }
    }

    public void saveSubmissionConfig(Long submissionId, com.edu.oj.entity.SubmissionConfig config) throws IOException {
        submissionFileManager.saveSubmissionConfig(submissionId, config);
    }
}
