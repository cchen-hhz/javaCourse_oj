package com.edu.oj.executor;

public enum LanguageType {
    JAVA(".java"),
    PYTHON(".py"),
    C(".c"),
    CPP(".cpp");

    private String extension;

    LanguageType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}