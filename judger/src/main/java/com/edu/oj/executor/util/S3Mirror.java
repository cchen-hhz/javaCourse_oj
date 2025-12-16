package com.edu.oj.executor.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3Mirror {

    public static void downloadPrefix(S3Client s3, String bucket, String prefix, Path destDir) {
        String marker = null;
        do {
            ListObjectsRequest.Builder reqBuilder = ListObjectsRequest.builder()
                    .bucket(bucket)
                    .prefix(prefix);

            if (marker != null && !marker.isEmpty()) {
                reqBuilder = reqBuilder.marker(marker);
            }

            ListObjectsResponse resp = s3.listObjects(reqBuilder.build());
            List<S3Object> objects = resp.contents();
            if (objects == null || objects.isEmpty()) {
                break;
            }

            for (S3Object obj : objects) {
                String key = obj.key();
                if (key == null || key.isEmpty()) {
                    continue;
                }
                if (!key.startsWith(prefix)) {
                    continue;
                }
                if (key.endsWith("/")) {
                    continue;
                }

                Path target = destDir.resolve(key.substring(prefix.length()));
                ensureParent(target);

                s3.getObject(
                        GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build(),
                        ResponseTransformer.toFile(target)
                );
            }

            marker = resp.isTruncated() ? resp.nextMarker() : null;
        } while (marker != null && !marker.isEmpty());
    }

    private static void ensureParent(Path p) {
        try {
            Path parent = p.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
