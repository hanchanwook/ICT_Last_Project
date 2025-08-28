package com.jakdang.labs.api.post.repository;

import com.jakdang.labs.api.post.entity.PostView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostViewRepository extends JpaRepository<PostView, String> {
    
    /**
     * 사용자와 게시글 ID로 조회 기록을 찾습니다.
     */
    Optional<PostView> findByUserIdAndPostId(String userId, String postId);
    
    /**
     * 사용자와 게시글 ID로 조회 기록이 존재하는지 확인합니다.
     */
    boolean existsByUserIdAndPostId(String userId, String postId);
} 