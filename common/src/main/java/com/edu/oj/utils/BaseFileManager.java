package com.edu.oj.utils;

import com.edu.oj.exceptions.BusinessException;
import com.edu.oj.exceptions.CommonErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 文件管理器
 */
@Slf4j
public abstract class BaseFileManager {

    protected final ObjectMapper objectMapper;

    protected BaseFileManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 读取配置文件
     * @param filePath 文件路径
     * @param clazz 配置类类型
     * @param resourceType 资源类型（用于异常信息）
     * @param resourceId 资源ID（用于异常信息）
     * @return 配置对象
     * @throws IOException 读取失败
     */
    protected <T> T readConfig(Path filePath, Class<T> clazz, String resourceType, Object resourceId) throws IOException {
        if (!Files.exists(filePath)) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, String.format("%s not found: %s", resourceType, resourceId));
        }
        return objectMapper.readValue(filePath.toFile(), clazz);
    }

    /**
     * 原子写入配置文件
     * @param filePath 目标文件路径
     * @param config 配置对象
     * @throws IOException 写入失败
     */
    protected void writeConfig(Path filePath, Object config) throws IOException {
        Path parentDir = filePath.getParent();
        
        // 确保目录存在
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        // 写入临时文件
        Path tempFile = Files.createTempFile(parentDir, "config-", ".tmp");
        
        try {
            log.info("写入文件" + filePath);
            objectMapper.writeValue(tempFile.toFile(), config);
            // 移动覆盖
            log.info("移动临时文件" + tempFile + "到" + filePath);
            Files.move(tempFile, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

            log.info("写入文件" + filePath + "done");
        } catch (IOException e) {
            log.error("写入配置文件异常: " + e.getMessage() + "尝试回退");
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                log.error("清理文件" + tempFile + "异常" + ignored.getMessage());
            }
            throw e;
        }
    }
}
