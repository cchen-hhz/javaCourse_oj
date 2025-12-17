package com.edu.oj.executor;

import com.edu.oj.entity.ProblemConfig;
import com.edu.oj.manager.FileSystemManager;
import com.edu.oj.message.ResultMessage;
import com.edu.oj.message.SubmissionMessage;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JudgeExecutor {

    public static final long ST_ACCEPTED = 0;
    public static final long ST_WRONG_ANSWER = 1;
    public static final long ST_TIME_LIMIT = 2;
    public static final long ST_MEMORY_LIMIT = 3;
    public static final long ST_RUNTIME_ERROR = 4;
    public static final long ST_WAITING = 7;
    public static final long ST_COMPILE_ERROR = -1;
    public static final long ST_SYSTEM_ERROR = -2;

    private final FileSystemManager fsm;

    public JudgeExecutor(FileSystemManager fsm) {
        this.fsm = fsm;
    }

    public void judge(SubmissionMessage sm, Consumer<ResultMessage> out) {
        ResultMessage start = new ResultMessage();
        start.setSubmissionId(sm.submissionId);
        start.setProblemId(sm.problemId);
        start.setTestCaseId(0L);
        start.setStatus(ST_WAITING);
        start.setCorrect(true);
        start.setIsOver(false);
        start.setMessage("judge_start");
        out.accept(start);

        Path probRoot = null;
        Path work = null;

        try {
            probRoot = fsm.extractProblemToTemp(sm.problemId);
            ProblemConfig pc = fsm.getProblemConfig(sm.problemId);
            long timeLimitMs = pc.getTime_limit() == null ? 1000 : pc.getTime_limit();
            long memoryLimitMb = pc.getMemory_limit() == null ? 256 : pc.getMemory_limit();

            Path tcDir = probRoot.resolve("testcases");
            List<Path> inputs;
            try (Stream<Path> s = Files.list(tcDir)) {
                inputs = s.filter(p -> p.getFileName().toString().endsWith(".in"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                        .collect(Collectors.toList());
            }

            long numCases = inputs.size();

            work = Files.createTempDirectory("judge_" + sm.submissionId + "_");
            Path code = work.resolve("code.cpp");
            String ext = FileSystemManager.getExtensionByLanguage(sm.language);
            try (InputStream is = fsm.getSubmissionFileStream(sm.submissionId, "code." + ext)) {
                Files.copy(is, code, StandardCopyOption.REPLACE_EXISTING);
            }

            if (!isCpp(sm.language)) {
                ResultMessage rm = systemError(sm, "only_cpp_supported");
                rm.setNumCases(numCases);
                rm.setIsOver(true);
                out.accept(rm);
                return;
            }

            Path exe = work.resolve("main");
            CompileResult cr = compileCpp(code, exe, 10_000);

            ResultMessage compileMsg = new ResultMessage();
            compileMsg.setSubmissionId(sm.submissionId);
            compileMsg.setProblemId(sm.problemId);
            compileMsg.setTestCaseId(0L);
            compileMsg.setNumCases(numCases);
            compileMsg.setTimeUsed(0L);
            compileMsg.setMemoryUsed(0L);
            compileMsg.setCorrect(true);

            if (!cr.ok) {
                compileMsg.setStatus(ST_COMPILE_ERROR);
                compileMsg.setMessage(trunc(cr.log, 8000));
                compileMsg.setIsOver(true);
                out.accept(compileMsg);
                return;
            } else {
                compileMsg.setStatus(ST_WAITING);
                compileMsg.setMessage("compile_ok");
                compileMsg.setIsOver(false);
                out.accept(compileMsg);
            }

            if (numCases == 0) {
                ResultMessage done = new ResultMessage();
                done.setSubmissionId(sm.submissionId);
                done.setProblemId(sm.problemId);
                done.setTestCaseId(0L);
                done.setNumCases(0L);
                done.setScore(0L);
                done.setTimeUsed(0L);
                done.setMemoryUsed(0L);
                done.setStatus(ST_SYSTEM_ERROR);
                done.setMessage("no_testcases");
                done.setCorrect(false);
                done.setIsOver(true);
                out.accept(done);
                return;
            }

            long totalScore = 0;
            long perScore = 100 / numCases;

            for (int i = 0; i < inputs.size(); i++) {
                long tcId = i + 1;
                Path in = inputs.get(i);
                Path exp = tcDir.resolve(stripExt(in.getFileName().toString()) + ".out");
                Path userOut = work.resolve("user_" + tcId + ".out");

                SandboxRunner.RunResult rr = SandboxRunner.run(exe, in, userOut, timeLimitMs, memoryLimitMb);

                String inputStr = safeRead(in, 4096);
                String expStr = Files.exists(exp) ? safeRead(exp, 4096) : "";
                String usrStr = Files.exists(userOut) ? safeRead(userOut, 4096) : "";

                long status;
                String msg;

                if (rr.tle) {
                    status = ST_TIME_LIMIT;
                    msg = "time_limit_exceeded";
                } else if (rr.mle) {
                    status = ST_MEMORY_LIMIT;
                    msg = "memory_limit_exceeded";
                } else if (!rr.ok) {
                    status = ST_RUNTIME_ERROR;
                    msg = "runtime_error(exit=" + rr.exitCode + ")";
                } else {
                    boolean same = OutputComparator.sameOutput(exp, userOut);
                    if (same) {
                        status = ST_ACCEPTED;
                        msg = "accepted";
                        totalScore += perScore;
                    } else {
                        status = ST_WRONG_ANSWER;
                        msg = "wrong_answer";
                    }
                }

                ResultMessage rm = new ResultMessage();
                rm.setSubmissionId(sm.submissionId);
                rm.setProblemId(sm.problemId);
                rm.setTestCaseId(tcId);
                rm.setNumCases(numCases);
                rm.setScore(totalScore);
                rm.setTimeUsed(rr.timeMs);
                rm.setMemoryUsed(rr.rssKb);
                rm.setStatus(status);
                rm.setInput(inputStr);
                rm.setExpectedOutput(expStr);
                rm.setUserOutput(usrStr);
                rm.setMessage(msg);
                rm.setCorrect(true);
                rm.setIsOver(false);
                out.accept(rm);
            }

            ResultMessage finalMsg = new ResultMessage();
            finalMsg.setSubmissionId(sm.submissionId);
            finalMsg.setProblemId(sm.problemId);
            finalMsg.setTestCaseId(numCases);
            finalMsg.setNumCases(numCases);
            finalMsg.setScore(totalScore);
            finalMsg.setTimeUsed(0L);
            finalMsg.setMemoryUsed(0L);
            finalMsg.setStatus(ST_ACCEPTED);
            finalMsg.setMessage("judge_done");
            finalMsg.setCorrect(true);
            finalMsg.setIsOver(true);
            out.accept(finalMsg);

        } catch (Exception e) {
            ResultMessage rm = systemError(sm, "system_error: " + e.getMessage());
            out.accept(rm);
        } finally {
            cleanup(probRoot, work);
        }
    }

    public static ResultMessage systemError(SubmissionMessage sm, String msg) {
        ResultMessage rm = new ResultMessage();
        rm.setSubmissionId(sm.submissionId);
        rm.setProblemId(sm.problemId);
        rm.setTestCaseId(0L);
        rm.setStatus(ST_SYSTEM_ERROR);
        rm.setMessage(msg);
        rm.setCorrect(false);
        rm.setIsOver(true);
        rm.setTimeUsed(0L);
        rm.setMemoryUsed(0L);
        rm.setScore(0L);
        return rm;
    }

    private static boolean isCpp(String lang) {
        if (lang == null) return true;
        String x = lang.toLowerCase();
        return x.equals("cpp") || x.equals("c++");
    }

    private static CompileResult compileCpp(Path code, Path exe, long timeoutMs) throws Exception {
        List<String> cmd = Arrays.asList(
                "bash", "-lc",
                "g++ -O2 -std=c++17 -pipe " + shell(code) + " -o " + shell(exe) + " 2>&1"
        );
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = p.getInputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) >= 0) baos.write(buf, 0, n);
        }

        boolean ok = p.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!ok) {
            p.destroyForcibly();
            return new CompileResult(false, "compile_timeout");
        }
        int ec = p.exitValue();
        return new CompileResult(ec == 0, baos.toString(StandardCharsets.UTF_8));
    }

    private static String stripExt(String n) {
        int i = n.lastIndexOf('.');
        return (i < 0) ? n : n.substring(0, i);
    }

    private static String safeRead(Path p, int max) {
        try (InputStream is = Files.newInputStream(p)) {
            byte[] buf = is.readNBytes(max);
            return new String(buf, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static String trunc(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }

    private static void cleanup(Path a, Path b) {
        deleteTree(a);
        deleteTree(b);
    }

    private static void deleteTree(Path p) {
        if (p == null) return;
        try {
            if (!Files.exists(p)) return;
            Files.walk(p)
                    .sorted(Comparator.reverseOrder())
                    .forEach(x -> { try { Files.deleteIfExists(x); } catch (Exception ignored) {} });
        } catch (Exception ignored) {
        }
    }

    private static String shell(Path p) {
        return "'" + p.toAbsolutePath().toString().replace("'", "'\"'\"'") + "'";
    }

    private static class CompileResult {
        final boolean ok;
        final String log;
        CompileResult(boolean ok, String log) { this.ok = ok; this.log = log; }
    }
}
