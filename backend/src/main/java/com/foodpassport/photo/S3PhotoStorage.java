package com.foodpassport.photo;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Photos in an S3 bucket. The bucket stays private: clients get a presigned URL - a link
 * signed with our AWS credentials that works for a few minutes, then expires.
 * Credentials come from the AWS default chain (env vars, ~/.aws/credentials, or an IAM role).
 */
@Component
@ConditionalOnProperty(name = "foodpassport.storage.type", havingValue = "s3")
public class S3PhotoStorage implements PhotoStorage {

    private static final Duration URL_LIFETIME = Duration.ofMinutes(10);

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    public S3PhotoStorage(@Value("${foodpassport.storage.s3.bucket}") String bucket,
                          @Value("${foodpassport.storage.s3.region}") String region) {
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("Set foodpassport.storage.s3.bucket (env S3_BUCKET) to use S3 storage");
        }
        this.bucket = bucket;
        this.s3 = S3Client.builder().region(Region.of(region)).build();
        this.presigner = S3Presigner.builder().region(Region.of(region)).build();
    }

    @Override
    public void store(String key, InputStream content, long size, String contentType) {
        s3.putObject(b -> b.bucket(bucket).key(key).contentType(contentType),
                RequestBody.fromInputStream(content, size));
    }

    @Override
    public Optional<URI> directUrl(String key) {
        var presigned = presigner.presignGetObject(b -> b
                .signatureDuration(URL_LIFETIME)
                .getObjectRequest(r -> r.bucket(bucket).key(key)));
        try {
            return Optional.of(presigned.url().toURI());
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Resource load(String key) {
        return new InputStreamResource(s3.getObject(b -> b.bucket(bucket).key(key)));
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(b -> b.bucket(bucket).key(key));
    }
}
