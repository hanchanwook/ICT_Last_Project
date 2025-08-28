package com.jakdang.labs.api.jaegyeom.popup.dto;

import lombok.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopupDto {
    private String id;
    private String content;
    private String imageUrl;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    private String createdBy;
    private String updatedBy;

    private Instant createdAt;
    private Instant updatedAt;

    // === 추가된 필드 ===
    private String educationId;
}
