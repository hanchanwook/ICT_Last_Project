package com.jakdang.labs.api.post.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_view_history",
       indexes = {
           @Index(name = "idx_post_view_history_post_id_viewed_at", columnList = "post_id, viewed_at"),
           @Index(name = "idx_post_view_history_viewed_at", columnList = "viewed_at"),
           @Index(name = "idx_post_view_history_user_id", columnList = "user_id"),
           @Index(name = "idx_post_view_history_owner_id_viewed_at", columnList = "owner_id, viewed_at"),
           @Index(name = "idx_post_view_history_owner_id_post_id", columnList = "owner_id, post_id")
       })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostViewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "char(36)", nullable = false)
    private String id;

    @Column(name = "user_id", columnDefinition = "char(36)", nullable = false)
    private String userId;

    @Column(name = "post_id", columnDefinition = "char(36)", nullable = false)
    private String postId;

    @Column(name = "owner_id", columnDefinition = "varchar(255)")
    private String ownerId;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        if (this.viewedAt == null) {
            this.viewedAt = now;
        }
    }
} 