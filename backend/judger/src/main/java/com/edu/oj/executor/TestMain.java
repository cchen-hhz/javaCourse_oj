package com.edu.oj.executor;

import com.edu.oj.executor.codeRunner.CodeRunner;
import com.edu.oj.executor.codeRunner.DockerCodeRunner;
import com.edu.oj.judge.JudgeResult;

public class TestMain {
    public static void main(String[] args) {
//        if (args.length != 2) {
//            System.err.println("Usage: java -jar judger.jar <submissionId> <problemId>");
//            System.exit(1);
//        }
        long submissionId = 1;
        long problemId    = 1;

        CodeRunner runner = new DockerCodeRunner();
        CodeExecutor executor = new CodeExecutor(runner);

        JudgeResult result = executor.judge(submissionId, problemId);
        System.out.println("allAccepted = " + result.isAllAccepted());
        System.out.println(result.toString());
        if (result.getCaseResults() != null) {
            result.getCaseResults().forEach(tc -> {
                System.out.println(
                        "#" + tc.getIndex() + " " +
                                tc.getStatus() + " - " +
                                tc.getMessage() + " IN:"+ tc.getInputPreview()+
                                " OUT:"+ tc.getExpectedOutputPreview()+" GOT:"+ tc.getActualOutputPreview()
                );
            });
        } else {
            System.out.println("No test case results, maybe error: " + result.getMessage());
        }
    }
}