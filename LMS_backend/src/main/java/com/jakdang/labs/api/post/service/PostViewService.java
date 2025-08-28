package com.jakdang.labs.api.post.service;

import com.jakdang.labs.api.post.entity.PostView;
import com.jakdang.labs.api.post.entity.PostViewHistory;
import com.jakdang.labs.api.post.model.PostDTO;
import com.jakdang.labs.api.post.model.PostServiceClient;
import com.jakdang.labs.api.post.repository.PostViewHistoryRepository;
import com.jakdang.labs.api.post.repository.PostViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostViewService {

    private final PostViewRepository postViewRepository;
    private final PostViewHistoryRepository postViewHistoryRepository;
    private final PostServiceClient postServiceClient;

    /**
     * 게시글 조회수를 증가시킵니다.
     * 1시간 쿨타임을 적용하여 같은 사용자가 1시간 이내에 같은 게시글을 조회해도 조회수가 증가하지 않습니다.
     * 조회수가 실제로 증가할 때만 PostViewHistory에 기록을 남깁니다.
     * 
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 조회수가 실제로 증가했는지 여부
     */
    @Transactional
    public boolean increaseViewCount(String postId, String userId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            Optional<PostView> existingView = postViewRepository.findByUserIdAndPostId(userId, postId);
            
            boolean shouldIncreaseCount = false;

            if (existingView.isPresent()) {
                PostView postView = existingView.get();
                LocalDateTime lastViewedAt = postView.getViewedAt();
                
                // 1시간(60분) 쿨타임 체크
                if (lastViewedAt.plusSeconds(1).isAfter(now)) {
                    log.debug("조회수 증가 쿨타임 중: userId={}, postId={}, lastViewed={}", 
                             userId, postId, lastViewedAt);
                    return false; // 쿨타임 중이므로 조회수 증가하지 않음
                }

                // 쿨타임이 지났으므로 조회 시간 업데이트
                postView.setViewedAt(now);
                postViewRepository.save(postView);
                shouldIncreaseCount = true;
                
            } else {
                // 첫 조회이므로 새로운 기록 생성
                PostView newPostView = PostView.builder()
                        .userId(userId)
                        .postId(postId)
                        .viewedAt(now)
                        .build();
                postViewRepository.save(newPostView);
                shouldIncreaseCount = true;
            }

            // 조회수가 실제로 증가할 때만 PostViewHistory 기록
            if (shouldIncreaseCount) {
                // 게시물 정보 조회하여 ownerId 획득
                String ownerId = getOwnerIdByPostId(postId);
                
                PostViewHistory viewHistory = PostViewHistory.builder()
                        .userId(userId)
                        .postId(postId)
                        .ownerId(ownerId)
                        .viewedAt(now)
                        .build();
                postViewHistoryRepository.save(viewHistory);
                
                // post-service에 조회수 증가 요청
                postServiceClient.increaseViewCount(postId, userId);
                
                log.info("조회수 증가 및 이력 기록 완료: userId={}, postId={}, ownerId={}", userId, postId, ownerId);
                return true;
            } else {
                log.debug("쿨타임으로 인한 조회수 증가 없음: userId={}, postId={}", userId, postId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("조회수 증가 처리 중 오류 발생: userId={}, postId={}, error={}", 
                     userId, postId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 게시물 ID로 ownerId를 조회합니다.
     * 
     * @param postId 게시물 ID
     * @return ownerId (조회 실패 시 null)
     */
    private String getOwnerIdByPostId(String postId) {
        try {
            PostDTO post = postServiceClient.getPostById(postId);
            return post.getOwner_id();
        } catch (Exception e) {
            log.warn("게시물 ownerId 조회 실패: postId={}, error={}", postId, e.getMessage());
            return null;
        }
    }

    /**
     * 사용자의 특정 게시글 마지막 조회 시간을 조회합니다.
     * 
     * @param userId 사용자 ID
     * @param postId 게시글 ID
     * @return 마지막 조회 시간 (조회 기록이 없으면 null)
     */
    public LocalDateTime getLastViewTime(String userId, String postId) {
        return postViewRepository.findByUserIdAndPostId(userId, postId)
                .map(PostView::getViewedAt)
                .orElse(null);
    }

    /**
     * 다음 조회 가능 시간까지 남은 시간(분)을 계산합니다.
     * 
     * @param userId 사용자 ID
     * @param postId 게시글 ID
     * @return 남은 시간(분), 조회 가능하면 0
     */
    public long getRemainingCooldownMinutes(String userId, String postId) {
        LocalDateTime lastViewTime = getLastViewTime(userId, postId);
        if (lastViewTime == null) {
            return 0; // 조회 기록이 없으면 바로 조회 가능
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextAvailableTime = lastViewTime.plusHours(1);
        
        if (nextAvailableTime.isAfter(now)) {
            return java.time.Duration.between(now, nextAvailableTime).toMinutes();
        } else {
            return 0; // 쿨타임이 지나서 조회 가능
        }
    }
} 