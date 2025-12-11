package com.edu.oj.executor.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Data
@AllArgsConstructor
public class CodeFileInfo {
    private Path codePath;
    private Language language;

    public static CodeFileInfo detectCodeFile(Path submissionDir) throws IOException {
        Path py = submissionDir.resolve("code.py");
        Path java = submissionDir.resolve("code.java");
        Path c = submissionDir.resolve("code.c");
        Path cpp = submissionDir.resolve("code.cpp");

        if (Files.exists(py)) {
            return new CodeFileInfo(py, Language.PYTHON);
        } else if (Files.exists(java)) {
            return new CodeFileInfo(java, Language.JAVA);
        } else if (Files.exists(c)) {
            return new CodeFileInfo(c, Language.C);
        } else if (Files.exists(cpp)) {
            return new CodeFileInfo(cpp, Language.CPP);
        }
        return null;
    }
}