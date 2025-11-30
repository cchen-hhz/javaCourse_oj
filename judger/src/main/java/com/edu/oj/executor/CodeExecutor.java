package com.edu.oj.executor;

import java.io.*;
import java.util.*;

public class CodeExecutor {
    private static final String WORK_DIR = "/app"; // 容器内工作目录

    public ExecutionResult executeCode(String language, String codePath, String input) throws Exception {
        String dockerImage = getDockerImage(language); // 获取对应的镜像

        // 构建 Docker 命令（挂载用户代码目录，运行）
        ProcessBuilder processBuilder = new ProcessBuilder(
                "docker", "run", "--rm",
                "-v", codePath + ":" + WORK_DIR + "/code", // 挂载用户代码
                "-v", input + ":" + WORK_DIR + "/input", // 挂载输入文件
                dockerImage,                              // 运行镜像
                language, WORK_DIR + "/code"              // 指定运行命令
        );

        // 设置输入输出重定向
        processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
        processBuilder.redirectError(ProcessBuilder.Redirect.PIPE);

        // 启动容器并运行
        Process process = processBuilder.start();
        String output = captureOutput(process.getInputStream());
        String error = captureOutput(process.getErrorStream());

        int exitCode = process.waitFor();
        return new ExecutionResult(output, error, exitCode);
    }

    private String getDockerImage(String language) {
        switch (language.toLowerCase()) {
            case "java": return "java-runner";
            case "python": return "python-runner";
            case "c": return "c-runner";
            case "cpp": return "cpp-runner";
            default: throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    private String captureOutput(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        }
        return builder.toString();
    }
}