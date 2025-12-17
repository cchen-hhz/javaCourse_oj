package com.edu.oj.executor;

import com.edu.oj.manager.FileSystemManager;
import com.edu.oj.message.ResultMessage;
import com.edu.oj.message.SubmissionMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@SpringBootApplication(scanBasePackages = "com.edu.oj")
public class TestMain {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(TestMain.class, args);
        JudgeWorker worker = ctx.getBean(JudgeWorker.class);
        worker.runForever();
    }

    @org.springframework.stereotype.Component
    public static class JudgeWorker {

        private final FileSystemManager fsm;
        private final ObjectMapper om;

        public JudgeWorker(FileSystemManager fsm) {
            this.fsm = fsm;
            this.om = new ObjectMapper();
        }

        public void runForever() {
            String bootstrap = env("SPRING_KAFKA_BOOTSTRAP_SERVERS", env("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"));
            String inTopic = env("KAFKA_SUBMISSION_TOPIC", "submission");
            String outTopic = env("KAFKA_RESULT_TOPIC", "result");
            String groupId = env("KAFKA_GROUP_ID", "judge-worker");

            Properties cprops = new Properties();
            cprops.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
            cprops.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            cprops.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            cprops.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            cprops.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
            cprops.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            cprops.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, env("KAFKA_MAX_POLL", "4"));

            Properties pprops = new Properties();
            pprops.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
            pprops.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            pprops.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            pprops.put(ProducerConfig.ACKS_CONFIG, "all");

            int workers = Integer.parseInt(env("JUDGE_WORKERS", "2"));
            ExecutorService pool = Executors.newFixedThreadPool(workers);

            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(cprops);
                 KafkaProducer<String, String> producer = new KafkaProducer<>(pprops)) {

                consumer.subscribe(Collections.singletonList(inTopic));

                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                    if (records.isEmpty()) continue;

                    List<Future<?>> futures = new ArrayList<>();
                    for (ConsumerRecord<String, String> r : records) {
                        futures.add(pool.submit(() -> {
                            try {
                                SubmissionMessage sm = om.readValue(r.value(), SubmissionMessage.class);
                                JudgeExecutor exec = new JudgeExecutor(fsm);
                                exec.judge(sm, msg -> sendResult(producer, outTopic, sm.submissionId, msg));
                            } catch (Exception e) {
                                try {
                                    SubmissionMessage sm2 = om.readValue(r.value(), SubmissionMessage.class);
                                    ResultMessage rm = JudgeExecutor.systemError(sm2, "system_error: " + e.getMessage());
                                    sendResult(producer, outTopic, sm2.submissionId, rm);
                                } catch (Exception ignored) {
                                }
                            }
                        }));
                    }

                    for (Future<?> f : futures) {
                        try { f.get(); } catch (Exception ignored) {}
                    }
                    consumer.commitSync();
                }
            }
        }

        private void sendResult(KafkaProducer<String, String> producer, String topic, Long key, ResultMessage rm) {
            try {
                String v = om.writeValueAsString(rm);
                producer.send(new ProducerRecord<>(topic, String.valueOf(key), v));
                producer.flush();
            } catch (Exception ignored) {
            }
        }

        private String env(String k, String d) {
            String v = System.getenv(k);
            return (v == null || v.isBlank()) ? d : v;
        }
    }
}
