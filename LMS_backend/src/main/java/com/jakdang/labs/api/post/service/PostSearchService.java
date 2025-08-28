package com.jakdang.labs.api.post.service;

import com.jakdang.labs.api.post.entity.PostSearchIndex;
import com.jakdang.labs.api.post.model.CustomPageDTO;
import com.jakdang.labs.api.post.model.PostDTO;
import com.jakdang.labs.api.post.model.PostServiceClient;
import com.jakdang.labs.api.post.repository.PostSearchIndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PostSearchService {
    
    private final PostSearchIndexRepository searchIndexRepository;
    private final PostServiceClient postServiceClient;
    
    /**
     * 게시물 검색
     */
    public CustomPageDTO<PostDTO> searchPosts(String keyword, String searchType, 
                                            String communityId, String boardId, 
                                            List<String> boardIds, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostSearchIndex> searchResults;
        
        // 여러 게시판 동시 검색
        if (boardIds != null && !boardIds.isEmpty()) {
            searchResults = searchIndexRepository.searchAllByBoardIds(keyword, communityId, boardIds, pageable);
        } else {
            // 단일 조건 검색
            switch (searchType) {
                case "title":
                    searchResults = searchIndexRepository.searchByTitle(keyword, communityId, boardId, pageable);
                    break;
                case "content":
                    searchResults = searchIndexRepository.searchByContent(keyword, communityId, boardId, pageable);
                    break;
                case "author":
                    searchResults = searchIndexRepository.searchByAuthor(keyword, communityId, boardId, pageable);
                    break;
                default: // "all"
                    searchResults = searchIndexRepository.searchAll(keyword, communityId, boardId, pageable);
                    break;
            }
        }
        
        // PostID 목록으로 실제 게시물 가져오기
        List<String> postIds = searchResults.getContent().stream()
                .map(PostSearchIndex::getPostId)
                .collect(Collectors.toList());
        
        List<PostDTO> posts = postIds.stream()
                .map(postServiceClient::getPostById)
                .filter(post -> post != null)
                .collect(Collectors.toList());
        
        // CustomPageDTO 생성
        CustomPageDTO<PostDTO> result = new CustomPageDTO<>();
        result.setContent(posts);
        result.setTotalElements(searchResults.getTotalElements());
        result.setTotalPages(searchResults.getTotalPages());
        result.setNumber(searchResults.getNumber());
        result.setLast(searchResults.isLast());
        
        return result;
    }
    
    /**
     * 검색 인덱스 생성/업데이트
     */
    @Transactional
    public void upsertSearchIndex(String postId, String title, String content, 
                                 String authorName, String communityId, String boardId) {
        try {
            PostSearchIndex searchIndex = PostSearchIndex.builder()
                    .postId(postId)
                    .title(title)
                    .content(content)
                    .authorName(authorName)
                    .communityId(communityId)
                    .boardId(boardId)
                    .build();
                    
            searchIndexRepository.save(searchIndex);
            log.info("검색 인덱스 업데이트: {}", postId);
            
        } catch (Exception e) {
            log.error("검색 인덱스 업데이트 실패: {}", postId, e);
        }
    }
    
    /**
     * PostDTO로부터 검색 인덱스 생성
     */
    @Transactional
    public void createSearchIndex(PostDTO postDTO) {
        if (postDTO == null) {
            log.warn("PostDTO가 null입니다.");
            return;
        }
        
        try {
            // PostDTO로부터 정보 추출
            String postId = postDTO.getId();
            String title = postDTO.getTitle();
            String content = postDTO.getContents();
            String authorName = postDTO.getAuthor() != null ? postDTO.getAuthor().getName() : "알 수 없음";
            String communityId = postDTO.getOwner_id();
            
            // boardId 추출 (target_ids에서 board: 접두사를 가진 것 찾기)
            String boardId = null;
            if (postDTO.getTarget_ids() != null) {
                boardId = postDTO.getTarget_ids().stream()
                        .filter(targetId -> targetId.startsWith("board:"))
                        .map(targetId -> targetId.replace("board:", ""))
                        .findFirst()
                        .orElse(null);
            }
            
            upsertSearchIndex(postId, title, content, authorName, communityId, boardId);
            
        } catch (Exception e) {
            log.error("PostDTO로부터 검색 인덱스 생성 실패: {}", postDTO.getId(), e);
        }
    }
    
    /**
     * 검색 인덱스 삭제
     */
    @Transactional
    public void deleteSearchIndex(String postId) {
        try {
            searchIndexRepository.deleteById(postId);
            log.info("검색 인덱스 삭제: {}", postId);
        } catch (Exception e) {
            log.error("검색 인덱스 삭제 실패: {}", postId, e);
        }
    }
    
    /**
     * 작성자 이름 변경 시 일괄 업데이트
     */
    @Transactional
    public void updateAuthorName(String authorId, String newAuthorName) {
        // 실제로는 @Query 애노테이션으로 UPDATE 쿼리 작성
        // 지금은 간단히 로그만
        log.info("작성자 이름 업데이트 필요: {} -> {}", authorId, newAuthorName);
    }
} 