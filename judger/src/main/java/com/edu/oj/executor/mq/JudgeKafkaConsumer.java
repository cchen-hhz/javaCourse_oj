package com.edu.oj.executor.mq;

import com.edu.oj.api.dto.SubmissionMessage;
import com.edu.oj.executor.CodeExecutor;
import com.edu.oj.executor.codeRunner.CodeRunner;
import com.edu.oj.executor.codeRunner.DockerCodeRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JudgeKafkaConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CodeRunner codeRunner = new DockerCodeRunner();
    private final CodeExecutor codeExecutor = new CodeExecutor(codeRunner);

    @KafkaListener(topics = "submission-queue", groupId = "judge-group")
    public void handleSubmission(ConsumerRecord<String, String> record) throws Exception {
        String json = record.value();

        SubmissionMessage msg = objectMapper.readValue(json, SubmissionMessage.class);

        Long problemId = msg.getProblemId();
        Long submissionId = msg.getSubmissionId();

        codeExecutor.judge(problemId.intValue(), submissionId.intValue());
    }
}
