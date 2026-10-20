package com.foodpassport.photo;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import org.springframework.core.io.Resource;

/**
 * Where photo bytes live. The database only stores a "key" (like a file path); this interface
 * turns keys into bytes. Two implementations, chosen by foodpassport.storage.type:
 *   - local: a folder on disk, for development and tests (no AWS account needed)
 *   - s3:    an AWS S3 bucket, for production
 */
public interface PhotoStorage {

    void store(String key, InputStream content, long size, String contentType);

    /** A short-lived URL the client can download from directly, if the storage supports it. */
    Optional<URI> directUrl(String key);

    /** The bytes themselves, streamed through our server. */
    Resource load(String key);

    void delete(String key);
}
