package com.edu.oj.executor.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Data
public class ProblemConfig {

    /** 题目 ID，可选字段，用于校验/调试 */
    @JsonProperty("id")
    private Long problemId;

    /** 统一的时间限制（毫秒），对该题所有测试点生效 */
    @JsonProperty("time_limit")
    private Long timeLimitMs;

    /** 统一的内存限制（MB），对该题所有测试点生效 */
    @JsonProperty("memory_limit")
    private Long memoryLimitMb;

    /**
     * 方便 CodeExecutor 使用的：带默认值的限制
     */
    public Limits getLimitsWithDefault() {
        long t = (timeLimitMs != null) ? timeLimitMs : 1000L;
        long m = (memoryLimitMb != null) ? memoryLimitMb/256L : 256L;
        return new Limits(t, m);
    }

    @Data
    public static class Limits {
        private final long timeLimitMs;
        private final long memoryLimitMb;
    }

    public static ProblemConfig loadProblemConfig(long problemId) throws IOException {
        Path configPath = Paths.get("data", "problems", String.valueOf(problemId), "config.yml");
        ProblemConfig cfg;
        if (!Files.exists(configPath)) {
            System.out.println("checkout:"+problemId+" "+configPath.toString());
            // 配置文件不存在，给一个带默认限制的配置
            cfg = new ProblemConfig();
            cfg.setProblemId(problemId);
            // timeLimitMs / memoryLimitMb 保持 null，getLimitsWithDefault() 会兜底
            return cfg;
        }
        YAMLMapper mapper = new YAMLMapper();
        byte[] bytes = Files.readAllBytes(configPath);
        cfg = mapper.readValue(bytes, ProblemConfig.class);
        if (cfg.getProblemId() == null) {
            cfg.setProblemId(problemId);
        }
        return cfg;
    }
}