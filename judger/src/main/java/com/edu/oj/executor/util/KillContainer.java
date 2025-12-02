package com.edu.oj.executor.util;

import java.util.concurrent.TimeUnit;

import static com.edu.oj.executor.codeRunner.DockerCodeRunner.DOCKER;

public class KillContainer {
    public static void killContainer(String name) {
        try {
            new ProcessBuilder(DOCKER, "kill", name)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor(1, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }
}
