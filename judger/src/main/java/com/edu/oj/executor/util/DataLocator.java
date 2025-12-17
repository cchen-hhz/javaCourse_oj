package com.edu.oj.executor.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class DataLocator {
    private static URI rootUri;
    private static Region region;
    private static S3Client s3;

    public static void init(String dataRoot, Region reg) {
        rootUri = URI.create(Objects.requireNonNull(dataRoot));
        region = reg;
        if (isS3()) {
            s3 = S3Client.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
        }
    }

    public static boolean isS3() {
        return rootUri != null && "s3".equalsIgnoreCase(rootUri.getScheme());
    }

    public static Path materializeDir(String... parts) {
        if (!isS3()) {
            return Paths.get(stripFileScheme(rootUri.getSchemeSpecificPart()), parts);
        }
        String bucket = rootUri.getHost();
        String base = rootUri.getPath().startsWith("/") ? rootUri.getPath().substring(1) : rootUri.getPath();
        StringBuilder keyPrefix = new StringBuilder();
        if (!base.isEmpty()) keyPrefix.append(base.endsWith("/") ? base : base + "/");
        for (int i = 0; i < parts.length; i++) {
            keyPrefix.append(parts[i]);
            if (i + 1 < parts.length) keyPrefix.append("/");
        }
        if (keyPrefix.length() == 0 || keyPrefix.charAt(keyPrefix.length() - 1) != '/') keyPrefix.append("/");
        Path dest = createTempMirrorDir(parts);
        S3Mirror.downloadPrefix(s3, bucket, keyPrefix.toString(), dest);
        return dest;
    }

    private static String stripFileScheme(String ssp) {
        if (ssp == null) return "";
        if (ssp.startsWith("/") && System.getProperty("os.name").toLowerCase().contains("win")) {
            if (ssp.length() >= 3 && ssp.charAt(2) == ':') return ssp.substring(1);
        }
        return ssp;
    }

    private static Path createTempMirrorDir(String... parts) {
        try {
            StringBuilder name = new StringBuilder("mirror_");
            for (String p : parts) name.append(p.replace('/', '_')).append("_");
            Path dir = Files.createTempDirectory(name.toString());
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Path materializeFile(String... parts) {
        if (!isS3()) {
            return Paths.get(stripFileScheme(rootUri.getSchemeSpecificPart()), parts);
        }
        String bucket = rootUri.getHost();
        String base = rootUri.getPath().startsWith("/") ? rootUri.getPath().substring(1) : rootUri.getPath();
        StringBuilder key = new StringBuilder();
        if (!base.isEmpty()) key.append(base.endsWith("/") ? base : base + "/");
        for (int i = 0; i < parts.length; i++) {
            key.append(parts[i]);
            if (i + 1 < parts.length) key.append("/");
        }
        Path tmp;
        try {
            tmp = Files.createTempFile("s3_", "_" + parts[parts.length - 1].replace("/", "_"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key.toString()).build(), ResponseTransformer.toFile(tmp));
        return tmp;
    }
    // DataLocator.java 新增
    public static void init(String dataRoot, software.amazon.awssdk.regions.Region reg,
                            java.net.URI endpoint, String accessKey, String secretKey, boolean pathStyle) {
        rootUri = java.net.URI.create(java.util.Objects.requireNonNull(dataRoot));
        region = reg;

        software.amazon.awssdk.services.s3.S3ClientBuilder builder =
                software.amazon.awssdk.services.s3.S3Client.builder().region(region);

        if (accessKey != null && !accessKey.isEmpty()
                && secretKey != null && !secretKey.isEmpty()) {
            builder = builder.credentialsProvider(
                    software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                            software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(accessKey, secretKey)
                    )
            );
        } else {
            builder = builder.credentialsProvider(
                    software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider.create()
            );
        }

        boolean hasEndpoint = endpoint != null
                && endpoint.toString() != null
                && !endpoint.toString().isBlank()
                && endpoint.getScheme() != null;

        if (hasEndpoint) {
            builder = builder.endpointOverride(endpoint)
                    .serviceConfiguration(
                            software.amazon.awssdk.services.s3.S3Configuration.builder()
                                    .pathStyleAccessEnabled(pathStyle)
                                    .build()
                    );
        }

        s3 = builder.build();
    }

}
