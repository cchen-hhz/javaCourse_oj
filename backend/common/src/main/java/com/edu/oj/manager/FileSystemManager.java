package com.edu.oj.manager;

import com.edu.oj.config.S3Properties;
import com.edu.oj.entity.SubmissionConfig;
import com.edu.oj.exceptions.BusinessException;
import com.edu.oj.exceptions.CommonErrorCode;
import com.edu.oj.judge.ProblemConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Map;

/**
 * 文件系统
 */
@Component
@Slf4j
public class FileSystemManager {

    private static final Map<String, String> LANGUAGE_EXTENSION_MAP = Map.of(
            "cpp", "cpp",
            "c++", "cpp",
            "java", "java",
            "python", "py",
            "py", "py",
            "c", "c"
    );

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Properties s3Properties;

    @Autowired
    @Qualifier("yamlMapper")
    private ObjectMapper yamlMapper;

    @Autowired
    @Qualifier("jsonMapper")
    private ObjectMapper jsonMapper;

    /**
     * 保存题目压缩包
     * @param problemId 题目ID
     * @param zipStream 压缩文件流
     * @throws IOException IO异常
     */
    public void saveAndUnzipProblemData(Long problemId, InputStream zipStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        zipStream.transferTo(baos);
        byte[] zipBytes = baos.toByteArray();

        // 验证 ZIP
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            Set<String> entries = new HashSet<>();
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.add(entry.getName());
            }
            
            boolean hasConfig = entries.contains("config.yml") || entries.contains("config.yaml");
            boolean hasStatement = entries.contains("statement.md");
            boolean hasTestcases = entries.stream().anyMatch(s -> s.startsWith("testcases/"));

            if (!hasConfig || !hasStatement || !hasTestcases) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, "Invalid problem zip structure. Must contain config.yml, statement.md and testcases/");
            }
        }

        // 上传到 S3
        String key = "problem/" + problemId + ".zip";
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build(), RequestBody.fromBytes(zipBytes));
            log.info("Problem zip uploaded to S3: {}", key);
        } catch (S3Exception e) {
            log.error("Failed to upload problem zip to S3", e);
            throw new IOException("Failed to upload problem zip", e);
        }
    }

    /**
     * 保存提交的代码文件
     * @param submissionId 提交ID
     * @param code 代码内容
     * @param language 语言
     * @throws IOException IO异常
     */
    public void saveSubmissionCode(Long submissionId, String code, String language) throws IOException {
        String extension = getExtensionByLanguage(language);
        String key = "submission/" + submissionId + "/code." + extension;
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build(), RequestBody.fromString(code, StandardCharsets.UTF_8));
            log.info("Submission code saved for submissionId: {}", submissionId);
        } catch (S3Exception e) {
            throw new IOException("Failed to save submission code", e);
        }
    }

    public static String getExtensionByLanguage(String language) {
        if (language == null) {
            return "txt";
        }
        return LANGUAGE_EXTENSION_MAP.getOrDefault(language.toLowerCase(), "txt");
    }

    /**
     * 读取题目文件流
     * @param problemId 题目ID
     * @param relativePath 相对路径
     * @return 文件输入流
     * @throws IOException IO异常   
     */
    public InputStream getProblemFileStream(Long problemId, String relativePath) throws IOException {
        String key = "problem/" + problemId + ".zip";
        
        ResponseBytes<GetObjectResponse> objectBytes;
        try {
            objectBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
             throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "Problem zip not found");
        } catch (S3Exception e) {
            throw new IOException("Failed to download problem zip", e);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(objectBytes.asByteArray());
        ZipInputStream zis = new ZipInputStream(bais);
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().equals(relativePath)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                return new ByteArrayInputStream(out.toByteArray());
            }
        }
        
        throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "File not found in zip: " + relativePath);
    }

    /**
     * 读取提交文件流
     * @param submissionId 提交ID
     * @param relativePath 相对路径
     * @return 文件输入流
     * @throws IOException IO异常
     */
    public InputStream getSubmissionFileStream(Long submissionId, String relativePath) throws IOException {
        String key = "submission/" + submissionId + "/" + relativePath;
        try {
            return s3Client.getObject(GetObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "File not found: " + relativePath);
        } catch (S3Exception e) {
            throw new IOException("Failed to get submission file", e);
        }
    }

    public ProblemConfig getProblemConfig(Long problemId) throws IOException {
        try (InputStream is = getProblemFileStream(problemId, "config.yml")) {
            return yamlMapper.readValue(is, ProblemConfig.class);
        } catch (BusinessException e) {
             try (InputStream is = getProblemFileStream(problemId, "config.yaml")) {
                return yamlMapper.readValue(is, ProblemConfig.class);
            } catch (Exception ex) {
                throw e;
            }
        }
    }

    /**
     * 删除题目数据
     * @param problemId 题目ID
     */
    public void deleteProblemData(Long problemId) {
        String key = "problem/" + problemId + ".zip";
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build());
        } catch (S3Exception e) {
            log.error("Failed to delete problem data for problemId: " + problemId, e);
        }
    }

    public void saveSubmissionConfig(Long submissionId, SubmissionConfig config) throws IOException {
        String key = "submission/" + submissionId + "/result.json";
        String json = jsonMapper.writeValueAsString(config);
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build(), RequestBody.fromString(json, StandardCharsets.UTF_8));
        } catch (S3Exception e) {
            throw new IOException("Failed to save submission config", e);
        }
    }

    public SubmissionConfig getSubmissionConfig(Long submissionId) throws IOException {
        String key = "submission/" + submissionId + "/result.json";
        try {
            InputStream is = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build());
            return jsonMapper.readValue(is, SubmissionConfig.class);
        } catch (NoSuchKeyException e) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "Submission result not found");
        } catch (S3Exception e) {
            throw new IOException("Failed to get submission config", e);
        }
    }
}
