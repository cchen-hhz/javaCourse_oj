package com.edu.oj.executor;

import com.edu.oj.config.S3Properties;
import com.edu.oj.executor.codeRunner.CodeRunner;
import com.edu.oj.executor.codeRunner.LocalShellCodeRunner;
import com.edu.oj.executor.util.DataLocator;
import com.edu.oj.judge.JudgeResult;
import com.edu.oj.manager.FileSystemManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class TestMain {

    private static final String BUCKET = "oj-data";
    private static final String REGION = "ap-northeast-1";
    private static final String ENDPOINT = "http://localhost:8333";
    private static final String ACCESS_KEY = "admin";
    private static final String SECRET_KEY = "admin";

    private static final long PROBLEM_ID = 1;
    private static final String PROBLEM_ZIP = "D:\\text\\javaHW\\javaCourse_oj\\backend\\data\\problems\\1.zip";

    private static final long SUBMISSION_ID = 1;
    private static final String CODE_FILE = "D:\\text\\javaHW\\javaCourse_oj\\backend\\data\\submission\\1\\code.cpp";
    private static final String LANGUAGE = "cpp";

    public static void main(String[] args) throws Exception {
        System.out.println("OS=" + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("JAVA=" + System.getProperty("java.version"));

        S3Client s3Client = buildS3Client();

        S3Properties s3Properties = new S3Properties();
        s3Properties.setBucket(BUCKET);

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        ObjectMapper jsonMapper = new ObjectMapper();

        FileSystemManager fileSystemManager = new FileSystemManager();
        inject(fileSystemManager, "s3Client", s3Client);
        inject(fileSystemManager, "s3Properties", s3Properties);
        inject(fileSystemManager, "yamlMapper", yamlMapper);
        inject(fileSystemManager, "jsonMapper", jsonMapper);

        URI endpointUri = (ENDPOINT == null || ENDPOINT.isEmpty()) ? null : URI.create(ENDPOINT);

        DataLocator.init(
                "s3://" + BUCKET + "/",
                Region.of(REGION),
                endpointUri,
                ACCESS_KEY,
                SECRET_KEY,
                true
        );

        System.out.println("==== 1) UPLOAD PROBLEM ZIP & UNZIP (to S3) ====");
        Path problemZipPath = Path.of(PROBLEM_ZIP);
        if (!Files.exists(problemZipPath)) {
            throw new IllegalStateException("Problem zip not found: " + problemZipPath.toAbsolutePath());
        }
        try (InputStream is = Files.newInputStream(problemZipPath)) {
            fileSystemManager.saveAndUnzipProblemData(PROBLEM_ID, is);
        }
        System.out.println("Uploaded & unzipped problemId=" + PROBLEM_ID);

        System.out.println("==== 2) UPLOAD SUBMISSION CODE (to S3) ====");
        Path codePath = Path.of(CODE_FILE);
        if (!Files.exists(codePath)) {
            throw new IllegalStateException("Code file not found: " + codePath.toAbsolutePath());
        }
        String code = Files.readString(codePath, StandardCharsets.UTF_8);
        fileSystemManager.saveSubmissionCode(SUBMISSION_ID, code, LANGUAGE);
        System.out.println("Uploaded submissionId=" + SUBMISSION_ID + " language=" + LANGUAGE);

        System.out.println("==== 3) DOWNLOAD CHECK: READ BACK SUBMISSION FILE STREAM ====");
        String ext = FileSystemManager.getExtensionByLanguage(LANGUAGE);
        try (InputStream in = fileSystemManager.getSubmissionFileStream(SUBMISSION_ID, "code." + ext)) {
            printFewLines(in, 20);
        }

        System.out.println("==== 4) DOWNLOAD TO TEMP: EXTRACT PROBLEM TO TEMP DIR ====");
        Path problemDir = fileSystemManager.extractProblemToTemp(PROBLEM_ID);
        System.out.println("Problem temp dir=" + problemDir.toAbsolutePath());
        listFew(problemDir, 80);

        System.out.println("==== 5) DOWNLOAD TO TEMP: MATERIALIZE SUBMISSION DIR ====");
        Path submissionDir = DataLocator.materializeDir("submission", SUBMISSION_ID);
        System.out.println("Submission materialized dir=" + submissionDir.toAbsolutePath());
        listFew(submissionDir, 80);

        System.out.println("==== 6) RUN CodeExecutor.judge(submissionId, problemId) ====");
        CodeRunner runner = new LocalShellCodeRunner();
        CodeExecutor executor = new CodeExecutor(runner);
        JudgeResult result = executor.judge(SUBMISSION_ID, PROBLEM_ID);

        System.out.println("==== FINAL JUDGE RESULT ====");
        System.out.println(result);
    }

    private static S3Client buildS3Client() {
        Region region = REGION != null && !REGION.isEmpty() ? Region.of(REGION) : Region.US_EAST_1;
        S3ClientBuilder builder = S3Client.builder().region(region);

        if (ACCESS_KEY != null && !ACCESS_KEY.isEmpty() && SECRET_KEY != null && !SECRET_KEY.isEmpty()) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)
                    )
            );
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        if (ENDPOINT != null && !ENDPOINT.isEmpty()) {
            S3Configuration config = S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .chunkedEncodingEnabled(false)
                    .build();

            builder.endpointOverride(URI.create(ENDPOINT))
                    .serviceConfiguration(config);
        }

        return builder.build();
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Class<?> cls = target.getClass();
        Field f;
        while (true) {
            try {
                f = cls.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
                if (cls == null) throw e;
            }
        }
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void listFew(Path dir, int max) throws Exception {
        if (dir == null || !Files.exists(dir)) {
            System.out.println("DIR not exists: " + dir);
            return;
        }
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.comparing(Path::toString))
                    .limit(max)
                    .forEach(p -> System.out.println("FILE " + dir.relativize(p)));
        }
    }

    private static void printFewLines(InputStream in, int maxLines) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null && count < maxLines) {
                System.out.println(line);
                count++;
            }
        }
    }
}
