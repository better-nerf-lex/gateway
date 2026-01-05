package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.exception.S3OperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String generatePresignedUrl(String s3Url) {
        try {
            String key = parseKey(s3Url);

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(60))
                    .getObjectRequest(builder -> builder.bucket(bucketName).key(key).build())
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for {}: {}", s3Url, e.getMessage());
            throw new S3OperationException("Failed to generate view URL", e);
        }
    }

    private String parseKey(String s3Url) {
        if (s3Url == null || !s3Url.startsWith("s3://")) {
            throw new IllegalArgumentException("Invalid S3 URL: " + s3Url);
        }
        // Remove s3://bucket/
        String path = s3Url.substring(5);
        int firstSlash = path.indexOf('/');
        if (firstSlash == -1) {
            return "";
        }
        return path.substring(firstSlash + 1);
    }
}
