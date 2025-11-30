package com.edu.oj.executor;

public class TestMain {
    public static void main(String[] args) {
        long submissionId = 1L;
        long problemId = 1L;

        CodeExecutor.CodeRunner runner = new DockerCodeRunner();
        CodeExecutor executor = new CodeExecutor(runner);

        CodeExecutor.JudgeResult result = executor.judge(submissionId, problemId);

        System.out.println("Judge success = " + result.isSuccess());
        System.out.println("All AC = " + result.isAllAccepted());
        System.out.println("Message = " + result.getMessage());
        result.getCaseResults().forEach(System.out::println);
    }
}