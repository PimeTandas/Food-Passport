package com.foodpassport.photo;

import com.foodpassport.auth.CurrentUser;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class PhotoController {

    private final PhotoService photos;
    private final PhotoStorage storage;

    public PhotoController(PhotoService photos, PhotoStorage storage) {
        this.photos = photos;
        this.storage = storage;
    }

    /** multipart/form-data with a "file" field. */
    @PostMapping(path = "/api/stamps/{stampId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PhotoResponse upload(@AuthenticationPrincipal Jwt jwt, @PathVariable Long stampId,
                                @RequestParam("file") MultipartFile file) {
        return photos.upload(CurrentUser.id(jwt), stampId, file);
    }

    /** S3: 302 redirect to a short-lived signed URL. Local disk: the image bytes. */
    @GetMapping("/api/photos/{id}")
    public ResponseEntity<Resource> get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Photo photo = photos.findViewable(CurrentUser.id(jwt), id);
        return storage.directUrl(photo.getStorageKey())
                .<ResponseEntity<Resource>>map(url -> ResponseEntity.status(HttpStatus.FOUND).location(url).build())
                .orElseGet(() -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(photo.getContentType()))
                        .body(storage.load(photo.getStorageKey())));
    }

    @DeleteMapping("/api/photos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        photos.delete(CurrentUser.id(jwt), id);
    }
}
