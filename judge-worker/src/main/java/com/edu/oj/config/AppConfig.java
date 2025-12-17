package com.edu.oj.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class AppConfig {

//    @Bean
//    public S3Client s3Client(S3Properties props) {
//        String endpoint = props.getEndpoint();
//        String region = (props.getRegion() == null || props.getRegion().isBlank()) ? "ap-northeast-1" : props.getRegion();
//
//        AwsBasicCredentials cred = AwsBasicCredentials.create(
//                props.getAccessKey() == null ? "" : props.getAccessKey(),
//                props.getSecretKey() == null ? "" : props.getSecretKey()
//        );
//
//        S3Configuration s3cfg = S3Configuration.builder()
//                .pathStyleAccessEnabled(true)
//                .build();
//
//        S3ClientBuilder b = S3Client.builder()
//                .credentialsProvider(StaticCredentialsProvider.create(cred))
//                .region(Region.of(region))
//                .serviceConfiguration(s3cfg);
//
//        if (endpoint != null && !endpoint.isBlank()) {
//            b.endpointOverride(URI.create(endpoint));
//        }
//        return b.build();
//    }

}
