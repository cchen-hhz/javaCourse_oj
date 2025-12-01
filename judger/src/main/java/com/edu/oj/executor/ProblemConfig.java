package com.edu.oj.executor;

import lombok.Data;

@Data
public class ProblemConfig {

    /** 题目 ID，可选字段，用于校验/调试 */
    private Long problemId;

    /** 统一的时间限制（毫秒），对该题所有测试点生效 */
    private Long timeLimitMs;

    /** 统一的内存限制（MB），对该题所有测试点生效 */
    private Long memoryLimitMb;

    /**
     * 方便 CodeExecutor 使用的：带默认值的限制
     */
    public Limits getLimitsWithDefault() {
        long t = (timeLimitMs != null) ? timeLimitMs : 1000L;
        long m = (memoryLimitMb != null) ? memoryLimitMb : 256L;
        return new Limits(t, m);
    }

    @Data
    public static class Limits {
        private final long timeLimitMs;
        private final long memoryLimitMb;
    }
}