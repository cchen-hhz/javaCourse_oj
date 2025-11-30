package com.edu.oj.service;

import com.edu.oj.dto.JudgeResult;
import com.edu.oj.dto.TestCaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for executing submitted code and comparing results with expected output.
 * 
 * Code is read from: data/submission/{submissionId}/code.{extension}
 * Test cases are read from: data/problem/{problemId}/testCases/{id}.in
 * Expected output is read from: data/problem/{problemId}/testCases/{id}.out
 */
@Slf4j
@Service
public class CodeExecutor {

    @Value("${judger.data.base-path:data}")
    private String dataBasePath;

    @Value("${judger.docker.timeout-seconds:30}")
    private int dockerTimeoutSeconds;

    private static final int STATUS_ACCEPTED = 0;
    private static final int STATUS_WRONG_ANSWER = 1;
    private static final int STATUS_TIME_LIMIT_EXCEEDED = 2;
    private static final int STATUS_RUNTIME_ERROR = 4;
    private static final int STATUS_SYSTEM_ERROR = 6;

    /**
     * Execute code for a given submission against all test cases for a problem.
     *
     * @param submissionId The ID of the submission
     * @param problemId    The ID of the problem
     * @param language     The programming language (py, java, c)
     * @return JudgeResult containing the overall result and individual test case results
     */
    public JudgeResult execute(Long submissionId, Long problemId, String language) {
        log.info("Starting execution for submission {} on problem {} with language {}", 
                submissionId, problemId, language);

        try {
            // Read code file
            String codePath = getCodePath(submissionId, language);
            if (!Files.exists(Paths.get(codePath))) {
                log.error("Code file not found: {}", codePath);
                return buildErrorResult(STATUS_SYSTEM_ERROR, "Code file not found: " + codePath);
            }

            // Get test cases directory
            Path testCasesDir = Paths.get(dataBasePath, "problem", String.valueOf(problemId), "testCases");
            if (!Files.exists(testCasesDir) || !Files.isDirectory(testCasesDir)) {
                log.error("Test cases directory not found: {}", testCasesDir);
                return buildErrorResult(STATUS_SYSTEM_ERROR, "Test cases directory not found");
            }

            // Find all test case input files
            List<Path> inputFiles = findInputFiles(testCasesDir);
            if (inputFiles.isEmpty()) {
                log.error("No test case input files found in: {}", testCasesDir);
                return buildErrorResult(STATUS_SYSTEM_ERROR, "No test cases found");
            }

            // Execute each test case
            List<TestCaseResult> testCaseResults = new ArrayList<>();
            int totalScore = 0;
            long maxTime = 0;
            long maxMemory = 0;
            int overallStatus = STATUS_ACCEPTED;

            for (int i = 0; i < inputFiles.size(); i++) {
                Path inputFile = inputFiles.get(i);
                Path outputFile = getExpectedOutputFile(inputFile);
                
                TestCaseResult result = executeTestCase(submissionId, language, inputFile, outputFile, i + 1);
                testCaseResults.add(result);

                if (result.getStatus() == STATUS_ACCEPTED) {
                    totalScore++;
                } else if (overallStatus == STATUS_ACCEPTED) {
                    overallStatus = result.getStatus();
                }

                maxTime = Math.max(maxTime, result.getTime());
                maxMemory = Math.max(maxMemory, result.getMemory());
            }

            return JudgeResult.builder()
                    .status(overallStatus)
                    .score(totalScore)
                    .timeUsed(maxTime)
                    .memoryUsed(maxMemory)
                    .compileMessage("")
                    .judgeMessage("")
                    .testCases(testCaseResults)
                    .build();

        } catch (Exception e) {
            log.error("Error executing submission {}: {}", submissionId, e.getMessage(), e);
            return buildErrorResult(STATUS_SYSTEM_ERROR, "System error: " + e.getMessage());
        }
    }

    /**
     * Execute a single test case.
     */
    private TestCaseResult executeTestCase(Long submissionId, String language, 
                                           Path inputFile, Path expectedOutputFile, int caseId) {
        log.debug("Executing test case {} for submission {}", caseId, submissionId);

        try {
            // Read input data
            String input = Files.readString(inputFile, StandardCharsets.UTF_8);
            
            // Read expected output
            String expectedOutput = "";
            if (Files.exists(expectedOutputFile)) {
                expectedOutput = Files.readString(expectedOutputFile, StandardCharsets.UTF_8);
            } else {
                log.warn("Expected output file not found: {}", expectedOutputFile);
            }

            // Build docker command
            String dockerImage = getDockerImage(language);
            String codePath = getCodePath(submissionId, language);
            
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "-v", codePath + ":/code/code." + getFileExtension(language),
                    "-i",
                    dockerImage
            );
            
            processBuilder.redirectErrorStream(true);

            long startTime = System.currentTimeMillis();
            Process process = processBuilder.start();

            // Write input to process
            try (OutputStream os = process.getOutputStream()) {
                os.write(input.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // Wait for process with timeout
            boolean finished = process.waitFor(dockerTimeoutSeconds, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();
            long timeUsed = endTime - startTime;

            if (!finished) {
                process.destroyForcibly();
                return TestCaseResult.builder()
                        .caseId(caseId)
                        .status(STATUS_TIME_LIMIT_EXCEEDED)
                        .time(timeUsed)
                        .memory(0)
                        .input(input)
                        .userOutput("")
                        .expectedOutput(expectedOutput)
                        .message("Time Limit Exceeded")
                        .build();
            }

            // Read output
            String userOutput;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                userOutput = sb.toString().trim();
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return TestCaseResult.builder()
                        .caseId(caseId)
                        .status(STATUS_RUNTIME_ERROR)
                        .time(timeUsed)
                        .memory(0)
                        .input(input)
                        .userOutput(userOutput)
                        .expectedOutput(expectedOutput)
                        .message("Runtime Error (exit code: " + exitCode + ")")
                        .build();
            }

            // Compare output
            boolean isCorrect = compareOutput(userOutput, expectedOutput.trim());
            
            return TestCaseResult.builder()
                    .caseId(caseId)
                    .status(isCorrect ? STATUS_ACCEPTED : STATUS_WRONG_ANSWER)
                    .time(timeUsed)
                    .memory(0)
                    .input(input)
                    .userOutput(userOutput)
                    .expectedOutput(expectedOutput.trim())
                    .message(isCorrect ? "Accepted" : "Wrong Answer")
                    .build();

        } catch (Exception e) {
            log.error("Error executing test case {}: {}", caseId, e.getMessage(), e);
            return TestCaseResult.builder()
                    .caseId(caseId)
                    .status(STATUS_SYSTEM_ERROR)
                    .time(0)
                    .memory(0)
                    .input("")
                    .userOutput("")
                    .expectedOutput("")
                    .message("System Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Compare user output with expected output.
     * Trims whitespace and normalizes line endings for comparison.
     */
    private boolean compareOutput(String userOutput, String expectedOutput) {
        String normalizedUser = normalizeOutput(userOutput);
        String normalizedExpected = normalizeOutput(expectedOutput);
        return normalizedUser.equals(normalizedExpected);
    }

    /**
     * Normalize output by trimming whitespace and normalizing line endings.
     */
    private String normalizeOutput(String output) {
        if (output == null) {
            return "";
        }
        return output.trim().replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
    }

    /**
     * Get the path to the code file.
     */
    private String getCodePath(Long submissionId, String language) {
        String extension = getFileExtension(language);
        return Paths.get(dataBasePath, "submission", String.valueOf(submissionId), 
                "code." + extension).toAbsolutePath().toString();
    }

    /**
     * Get file extension based on language.
     */
    private String getFileExtension(String language) {
        if (language == null) {
            return "txt";
        }
        return switch (language.toLowerCase()) {
            case "python", "py" -> "py";
            case "java" -> "java";
            case "c" -> "c";
            case "cpp", "c++" -> "cpp";
            default -> language.toLowerCase();
        };
    }

    /**
     * Get Docker image name based on language.
     */
    private String getDockerImage(String language) {
        if (language == null) {
            return "oj-runner";
        }
        return switch (language.toLowerCase()) {
            case "python", "py" -> "oj-python";
            case "java" -> "oj-java";
            case "c" -> "oj-c";
            case "cpp", "c++" -> "oj-cpp";
            default -> "oj-runner";
        };
    }

    /**
     * Find all input files in test cases directory.
     */
    private List<Path> findInputFiles(Path testCasesDir) throws IOException {
        List<Path> inputFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(testCasesDir, "*.in")) {
            for (Path entry : stream) {
                inputFiles.add(entry);
            }
        }
        // Sort by filename to ensure consistent order
        inputFiles.sort((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));
        return inputFiles;
    }

    /**
     * Get the expected output file path for a given input file.
     */
    private Path getExpectedOutputFile(Path inputFile) {
        String inputFileName = inputFile.getFileName().toString();
        String outputFileName = inputFileName.replace(".in", ".out");
        return inputFile.getParent().resolve(outputFileName);
    }

    /**
     * Build an error result.
     */
    private JudgeResult buildErrorResult(int status, String message) {
        return JudgeResult.builder()
                .status(status)
                .score(0)
                .timeUsed(0)
                .memoryUsed(0)
                .compileMessage("")
                .judgeMessage(message)
                .testCases(new ArrayList<>())
                .build();
    }
}
