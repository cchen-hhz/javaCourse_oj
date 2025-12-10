package com.edu.oj;

import com.edu.oj.api.dto.JudgeTaskMessage;
import com.edu.oj.executor.CodeExecutor;
import com.edu.oj.executor.codeRunner.CodeRunner;
import com.edu.oj.executor.codeRunner.DockerCodeRunner;
import com.edu.oj.judge.JudgeResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class JudgerApplication {
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "judge-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("submission-queue"));

        ObjectMapper mapper = new ObjectMapper();
        CodeRunner codeRunner = new DockerCodeRunner();
        CodeExecutor executor = new CodeExecutor(codeRunner);

        System.out.println("JudgerApplication started, waiting messages from topic submission-queue...");

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<String, String> record : records) {
                String json = record.value();
                try {
                    JudgeTaskMessage msg = mapper.readValue(json, JudgeTaskMessage.class);
                    long submissionId = msg.getSubmissionId();
                    long problemId = msg.getProblemId();
                    System.out.println("Receive task: submissionId=" + submissionId + ", problemId=" + problemId);
                    JudgeResult result = executor.judge(submissionId, problemId);
                    System.out.println("Judge result: allAccepted=" + result.isAllAccepted()
                            + ", message=" + result.getMessage());
                } catch (Exception e) {
                    System.err.println("Handle message error, raw=" + json);
                    e.printStackTrace();
                }
            }
        }
    }
}
