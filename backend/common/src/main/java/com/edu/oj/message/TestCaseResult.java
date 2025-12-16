package com.edu.oj.message;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;

@Data
@AllArgsConstructor
public class TestCaseResult {
    private int index;
    private TestCaseStatus status;
    private String message;
    private Path inputFile;
    private Path outputFile;
    private String actualOutput;
    private String expectedOutput;

    private String inputPreview;
    private String expectedOutputPreview;
    private String actualOutputPreview;
//    public static TestCaseResult accepted(int index, Path in, Path out, String actual) {
//        return new TestCaseResult(index, TestCaseStatus.AC,
//                "Accepted", in, out, actual, null);
//    }
//
//    public static TestCaseResult wrongAnswer(int index, Path in, Path out,
//                                             String actual, String expected) {
//        return new TestCaseResult(index, TestCaseStatus.WA,
//                "Wrong Answer", in, out, actual, expected);
//    }
//
//    public static TestCaseResult runtimeError(int index, Path in, Path out, String msg) {
//        return new TestCaseResult(index, TestCaseStatus.RE,
//                "Runtime Error: " + msg, in, out, null, null);
//    }
//
//
//
//    public static TestCaseResult missingOutputFile(int index, Path in, Path out) {
//        return new TestCaseResult(index, TestCaseStatus.CONFIG_ERROR,
//                "Missing .out file: " + out, in, out, null, null);
//    }
}
