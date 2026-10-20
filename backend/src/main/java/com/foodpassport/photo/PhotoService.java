package com.foodpassport.photo;

import com.foodpassport.common.ApiException;
import com.foodpassport.stamp.FoodEntry;
import com.foodpassport.stamp.Stamp;
import com.foodpassport.stamp.StampRepository;
import com.foodpassport.user.UserRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class PhotoService {

    static final int MAX_PHOTOS_PER_ENTRY = 20;

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/heic", "heic",
            "image/heif", "heif");

    private final PhotoRepository photos;
    private final StampRepository stamps;
    private final UserRepository users;
    private final PhotoStorage storage;

    public PhotoService(PhotoRepository photos, StampRepository stamps, UserRepository users, PhotoStorage storage) {
        this.photos = photos;
        this.stamps = stamps;
        this.users = users;
        this.storage = storage;
    }

    /** Add a photo to the food entry behind one of my stamps. */
    public PhotoResponse upload(Long meId, Long stampId, MultipartFile file) {
        Stamp stamp = stamps.findByIdAndOwnerId(stampId, meId)
                .orElseThrow(() -> ApiException.notFound("Stamp " + stampId + " not found"));
        FoodEntry entry = stamp.getFoodEntry();

        String extension = file.getContentType() == null ? null : EXTENSIONS.get(file.getContentType());
        if (file.isEmpty() || extension == null) {
            throw ApiException.badRequest("Upload a JPEG, PNG, WebP or HEIC image");
        }
        if (entry.getPhotos().size() >= MAX_PHOTOS_PER_ENTRY) {
            throw ApiException.badRequest("An entry can have at most " + MAX_PHOTOS_PER_ENTRY + " photos");
        }

        String key = "entries/" + entry.getId() + "/" + UUID.randomUUID() + "." + extension;
        try (InputStream in = file.getInputStream()) {
            storage.store(key, in, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // If saving the database row fails, don't leave an orphaned file behind.
        afterRollback(() -> storage.delete(key));

        Photo photo = photos.save(new Photo(entry, users.getReferenceById(meId), key, file.getContentType()));
        return PhotoResponse.from(photo);
    }

    @Transactional(readOnly = true)
    public Photo findViewable(Long meId, Long photoId) {
        Photo photo = photos.findById(photoId)
                .orElseThrow(() -> ApiException.notFound("Photo " + photoId + " not found"));
        // You can see a photo if you have a stamp on its food entry. Otherwise pretend it doesn't exist.
        if (!stamps.existsByOwnerIdAndFoodEntryId(meId, photo.getFoodEntry().getId())) {
            throw ApiException.notFound("Photo " + photoId + " not found");
        }
        return photo;
    }

    /** The uploader or the entry's creator can remove a photo. */
    public void delete(Long meId, Long photoId) {
        Photo photo = findViewable(meId, photoId);
        boolean uploader = photo.getUploadedBy() != null && photo.getUploadedBy().getId().equals(meId);
        if (!uploader && !photo.getFoodEntry().isCreatedBy(meId)) {
            throw ApiException.forbidden("Only the uploader or the entry's creator can delete this photo");
        }
        photo.getFoodEntry().getPhotos().remove(photo);
        photos.delete(photo);
        afterCommit(List.of(photo.getStorageKey()));
    }

    /** Called when a food entry is deleted. */
    public void deleteAllFor(FoodEntry entry) {
        List<String> keys = entry.getPhotos().stream().map(Photo::getStorageKey).toList();
        photos.deleteAll(entry.getPhotos());
        entry.getPhotos().clear();
        afterCommit(keys);
    }

    /** Only delete the files once the database change is definitely saved. */
    private void afterCommit(List<String> keys) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                keys.forEach(storage::delete);
            }
        });
    }

    private void afterRollback(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    action.run();
                }
            }
        });
    }
}
