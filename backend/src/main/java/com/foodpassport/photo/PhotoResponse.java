package com.foodpassport.photo;

import com.foodpassport.user.UserSummary;
import java.time.Instant;

/** "url" is our own endpoint; it checks you're allowed to see the photo, then sends or redirects to it. */
public record PhotoResponse(Long id, String url, String contentType, UserSummary uploadedBy, Instant createdAt) {

    public static PhotoResponse from(Photo p) {
        return new PhotoResponse(p.getId(), "/api/photos/" + p.getId(), p.getContentType(),
                UserSummary.from(p.getUploadedBy()), p.getCreatedAt());
    }
}
