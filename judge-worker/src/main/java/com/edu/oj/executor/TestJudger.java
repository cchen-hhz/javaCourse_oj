package com.edu.oj.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

public class TestJudger {

    public static void main(String[] args) throws Exception {
        long problemId = 1L;
        long submissionId = System.currentTimeMillis();
        String language = "cpp";

        Path zipPath = Paths.get(System.getProperty("user.dir"), "1.zip");
        Path codePath = Paths.get(System.getProperty("user.dir"), "code.cpp");

        if (!Files.exists(zipPath)) {
            throw new RuntimeException("Missing file: " + zipPath.toAbsolutePath());
        }
        if (!Files.exists(codePath)) {
            throw new RuntimeException("Missing file: " + codePath.toAbsolutePath());
        }

        String kafkaBootstrap = env("SPRING_KAFKA_BOOTSTRAP_SERVERS", env("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"));
        String submissionTopic = env("KAFKA_SUBMISSION_TOPIC", "submission");
        String resultTopic = env("KAFKA_RESULT_TOPIC", "result");

        String s3Endpoint = env("S3_ENDPOINT", "http://localhost:8333");
        String s3Region = env("S3_REGION", "ap-northeast-1");
        String s3AccessKey = env("S3_ACCESS_KEY", "admin");
        String s3SecretKey = env("S3_SECRET_KEY", "admin");
        String s3Bucket = env("S3_BUCKET", "oj-data");

        S3Client s3 = buildS3(s3Endpoint, s3Region, s3AccessKey, s3SecretKey);
        ensureBucket(s3, s3Bucket);

        byte[] problemZipBytes = Files.readAllBytes(zipPath);
        byte[] codeBytes = Files.readAllBytes(codePath);

        String problemKey = "problem/" + problemId + ".zip";
        String codeKey = "submission/" + submissionId + "/code.cpp";

        putObjectBytes(s3, s3Bucket, problemKey, problemZipBytes, "application/zip");
        putObjectBytes(s3, s3Bucket, codeKey, codeBytes, "text/plain");

        ObjectMapper om = new ObjectMapper();

        Map<String, Object> submission = new LinkedHashMap<>();
        submission.put("submissionId", submissionId);
        submission.put("problemId", problemId);
        submission.put("language", language);
        String submissionJson = om.writeValueAsString(submission);

        Properties pprops = new Properties();
        pprops.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrap);
        pprops.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        pprops.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        pprops.put(ProducerConfig.ACKS_CONFIG, "all");

        Properties cprops = new Properties();
        cprops.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrap);
        cprops.put(ConsumerConfig.GROUP_ID_CONFIG, "test-judger-" + UUID.randomUUID());
        cprops.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        cprops.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        cprops.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        cprops.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(pprops);
             KafkaConsumer<String, String> consumer = new KafkaConsumer<>(cprops)) {

            consumer.subscribe(Collections.singletonList(resultTopic));

            producer.send(new ProducerRecord<>(submissionTopic, String.valueOf(submissionId), submissionJson)).get();
            producer.flush();

            System.out.println("SENT SubmissionMessage => " + submissionJson);
            System.out.println("LISTEN result topic => " + resultTopic);

            long start = System.currentTimeMillis();
            boolean done = false;

            while (!done) {
                ConsumerRecords<String, String> recs = consumer.poll(Duration.ofMillis(500));

                recs.forEach(r -> {
                    try {
                        Map<?, ?> m = om.readValue(r.value(), Map.class);
                        Object sid = m.get("submissionId");
                        if (sid == null) return;
                        if (!String.valueOf(sid).equals(String.valueOf(submissionId))) return;

                        Object tc = m.get("testCaseId");
                        Object st = m.get("status");
                        Object tu = m.get("timeUsed");
                        Object mu = m.get("memoryUsed");
                        Object msg = m.get("message");
                        Object over = m.get("isOver");

                        System.out.println("RESULT sid=" + sid +
                                " tc=" + tc +
                                " status=" + st +
                                " timeUsed=" + tu +
                                " memoryUsed=" + mu +
                                " isOver=" + over +
                                " message=" + msg);

                    } catch (Exception e) {
                        System.out.println("RAW RESULT => " + r.value());
                    }
                });

                for (var r : recs) {
                    try {
                        Map<?, ?> m = om.readValue(r.value(), Map.class);
                        Object sid = m.get("submissionId");
                        if (sid == null) continue;
                        if (!String.valueOf(sid).equals(String.valueOf(submissionId))) continue;
                        Object over = m.get("isOver");
                        if (over != null && Boolean.parseBoolean(String.valueOf(over))) {
                            done = true;
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (System.currentTimeMillis() - start > 120_000) {
                    System.out.println("TIMEOUT(120s). Check judge-worker logs / topics / S3 keys.");
                    break;
                }
            }
        }
    }

    private static S3Client buildS3(String endpoint, String region, String ak, String sk) {
        AwsBasicCredentials cred = AwsBasicCredentials.create(ak, sk);
        S3Configuration s3cfg = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(cred))
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(s3cfg)
                .build();
    }

    private static void ensureBucket(S3Client s3, String bucket) {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (Exception e) {
            try {
                s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            } catch (Exception ignored) {
            }
        }
    }

    private static void putObjectBytes(S3Client s3, String bucket, String key, byte[] data, String contentType) {
        s3.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(data)
        );
        System.out.println("S3 PUT => s3://" + bucket + "/" + key + " (" + data.length + " bytes)");
    }

    private static String env(String k, String d) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? d : v;
    }
}
