package com.jakdang.labs.api.post.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_views", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostView {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "char(36)", nullable = false)
    private String id;

    @Column(name = "user_id", columnDefinition = "char(36)", nullable = false)
    private String userId;

    @Column(name = "post_id", columnDefinition = "char(36)", nullable = false)
    private String postId;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.viewedAt == null) {
            this.viewedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
} 