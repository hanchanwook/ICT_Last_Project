package com.jakdang.labs.api.post.controller;

import com.jakdang.labs.api.auth.dto.CustomUserDetails;
import com.jakdang.labs.api.auth.dto.RoleType;
import com.jakdang.labs.api.auth.dto.UserDTO;
import com.jakdang.labs.api.auth.service.AuthService;
import com.jakdang.labs.api.common.ResponseDTO;
import com.jakdang.labs.api.post.PostUtil;
import com.jakdang.labs.api.post.dto.PostCountDTO;
import com.jakdang.labs.api.post.dto.PostViewResponseDTO;
import com.jakdang.labs.api.post.model.*;
import com.jakdang.labs.api.post.service.PostSearchService;
import com.jakdang.labs.api.post.service.PostViewService;
import com.jakdang.labs.exceptions.handler.CustomException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "게시글 API", description = "게시글 관련 API")
public class PostV2Controller {
    private final PostServiceClient postServiceClient;
    private final PostUtil postUtil;
    private final AuthService authService;
    private final PostViewService postViewService;
    private final PostSearchService postSearchService;

    /**
     * 글 1건 조회
     */
    @Operation(summary = "글 상세 조회", description = "ID로 게시글 1건을 조회합니다.")
    @GetMapping("/detail/{id}")
    public ResponseDTO<PostDTO> getPostById(
            @Parameter(description = "게시글 ID") @PathVariable("id") String id,
            @Parameter(description = "조회자 회원 ID") @RequestParam(value = "member_id", required = false) String memberId,
            @AuthenticationPrincipal CustomUserDetails userDetail
    ) {
        try {
            PostDTO postDTO = postServiceClient.getPostById(id);

            log.info("게시글 조회 - postId: {}, memberId: {}", id, memberId);
            log.info(postDTO.toString());
            // 게시물이 null인 경우 처리
            if (postDTO == null) {
                return ResponseDTO.createSuccessResponse("게시물을 찾을 수 없습니다.", null);
            }

            //멤버 정보를 추가로 등록 해줌
            postUtil.addAuthorToPost(postDTO);
            postUtil.addFilePost(postDTO);
            postUtil.addTargetClass(postDTO);

            return ResponseDTO.createSuccessResponse("ok", postDTO);

        } catch (Exception e) {
            log.error("게시글 조회 실패 - postId: {}, error: {}", id, e.getMessage());
            // 게시물이 없거나 오류가 발생한 경우 null 반환
            return ResponseDTO.createSuccessResponse("게시물을 찾을 수 없습니다.", null);
        }
    }

    /**
     * 글쓰기
     */
    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성합니다.")
    @PostMapping
    public ResponseDTO<String> createPost(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "게시글 생성 정보")
            @RequestBody PostCreateDTO postCreateDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        postCreateDTO.setAuthorId(userDetails.getUserId());
        PostDTO postDTO = postServiceClient.createPost(postCreateDTO);

        // 검색 보조 테이블에 인덱스 생성
        try {
            postUtil.addAuthorToPost(postDTO); // 작성자 정보 먼저 로드
            postSearchService.createSearchIndex(postDTO);
            log.info("검색 인덱스 생성 완료 - postId: {}", postDTO.getId());
        } catch (Exception e) {
            log.error("검색 인덱스 생성 실패 - postId: {}, error: {}", postDTO.getId(), e.getMessage());
        }



        return ResponseDTO.createSuccessResponse("create OK", postDTO.getId());
    }

    /**
     * 글 수정하기
     */
    @Operation(summary = "게시글 수정", description = "기존 게시글을 수정합니다.")
    @PutMapping("/{id}")
    public ResponseDTO<String> updatePost(
            @Parameter(description = "게시글 ID") @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "게시글 수정 정보")
            @RequestBody PostCreateDTO postCreateDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        PostDTO beforePost = postServiceClient.getPostById(id);
        if (userDetails.getRole() != RoleType.ROLE_ADMIN && !beforePost.getAuthor_id().equals(userDetails.getUserId())) {
            throw new CustomException("본인이 작성한 게시글이 아닙니다", -400);
        }

        PostDTO afterPost = postServiceClient.updatePost(id, postCreateDTO);

        // 검색 보조 테이블 업데이트
        try {
            postUtil.addAuthorToPost(afterPost); // 작성자 정보 먼저 로드
            postSearchService.createSearchIndex(afterPost);
            log.info("검색 인덱스 업데이트 완료 - postId: {}", afterPost.getId());
        } catch (Exception e) {
            log.error("검색 인덱스 업데이트 실패 - postId: {}, error: {}", afterPost.getId(), e.getMessage());
        }

        //임시저장인 상태에서 정상 게시가되었을때는 새소식, 푸시알림을 발송한다.
        if (!beforePost.is_published() && afterPost.is_published()) {
            // ===== 새소식, 푸시알림 ===== //
//            newsService.sendPostCreate(afterPost, member, postCreateDTO.getPostType());
        }

        return ResponseDTO.createSuccessResponse("update OK", afterPost.getId());
    }

    /**
     * 글 삭제
     */
    @Operation(summary = "게시글 삭제", description = "ID로 게시글을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseDTO<String> deletePostById(
            @Parameter(description = "게시글 ID") @PathVariable String id,
            @Parameter(description = "작성자 ID") @RequestParam("authorId") String authorId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
//        membersService.isTokenMember(userDetails, authorId);
        PostDTO post = postServiceClient.getPostById(id);
        if (post.getAuthor_id().equals(authorId)) {
            postServiceClient.deletePostById(id);

            // 검색 보조 테이블에서 인덱스 삭제
            try {
                postSearchService.deleteSearchIndex(id);
                log.info("검색 인덱스 삭제 완료 - postId: {}", id);
            } catch (Exception e) {
                log.error("검색 인덱스 삭제 실패 - postId: {}, error: {}", id, e.getMessage());
            }
        } else {
            throw new CustomException("본인 글이 아님", -200);
        }

        return ResponseDTO.createSuccessResponse("ok", "");
    }

    /**
     * 글 목록 조회 (페이징)
     */
    @Operation(summary = "게시글 목록 조회", description = "페이징 처리된 게시글 목록을 조회합니다.")
    @GetMapping
    public ResponseDTO<CustomPageDTO<PostDTO>> getPosts(
            @Parameter(description = "회원 ID") @RequestParam(value = "member_id", required = false) String memberId,
            @Parameter(description = "발행 여부") @RequestParam(value = "published", defaultValue = "true") Boolean published,
            @Parameter(description = "게시글 타입") @RequestParam(value = "postType", required = false) String postType,
            @Parameter(description = "클래스 ID") @RequestParam(value = "classId", required = false) String classId,
            @Parameter(description = "소유자 ID") @RequestParam(value = "ownerId", required = false) String ownerId,
            @Parameter(description = "게시판 ID 목록") @RequestParam(value = "boardIds", required = false) List<Long> boardIds,
            @Parameter(description = "타겟 ID 목록") @RequestParam(value = "targetIds", required = false) List<String> targetIds,
            @Parameter(description = "채널 ID") @RequestParam(value = "channelId", required = false) String channelId,
            @Parameter(description = "페이지 번호") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(description = "정렬 방식") @RequestParam(value = "sort", defaultValue = "publishedTime,desc") String sort,
            @Parameter(description = "사용자 ID") @RequestParam(value = "authorId", required = false) String authorId,
            @Parameter(description = "검색") @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "검색타입 : title, contents, both") @RequestParam(value = "searchType", required = false) String searchType,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        PostSearchCondition searchCondition = PostSearchCondition.builder()
                .published(published)
                .postType(postType)
                .boardIds(boardIds)
                .search(search)
                .targetIds(targetIds)
                .includeNull(false)
                .build();

        if (ownerId == null || ownerId.isEmpty()) {
            searchCondition.setOwnerId("");
        } else if (ownerId.equals("all")) {
            searchCondition.setOwnerId(null);
        } else {
            searchCondition.setOwnerId(ownerId);
        }

        if (StringUtils.hasText(authorId)) {
            searchCondition.setAuthorId(authorId);
        }

        if (StringUtils.hasText(channelId)) {
            searchCondition.setChannelId(channelId);
        }

        CustomPageDTO<PostDTO> customPageDTO = postServiceClient.getPosts(searchCondition, memberId, page, size, sort);

        //TODO N+1 개선 필요 -> 개선됨
        customPageDTO.getContent().forEach(it -> {
            postUtil.addAuthorToPost(it);
            postUtil.addFilePost(it);
            postUtil.addTargetClass(it);

        });

        return ResponseDTO.createSuccessResponse("success", customPageDTO);
    }

    /**
     * 북마크된 글 조회
     */
    @Operation(summary = "북마크된 글 조회", description = "사용자가 북마크한 게시글 목록을 조회합니다.")
    @GetMapping("/bookmarks")
    public ResponseDTO<CustomPageDTO<PostDTO>> getBookmarkedPost(
            @Parameter(description = "사용자 ID") @RequestParam("userId") String userId,
            @Parameter(description = "게시글 타입") @RequestParam("postType") String postType,
            @Parameter(description = "게시판 ID 목록") @RequestParam("boardIds") List<Long> boardIds,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 방식") @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

//        Members member = membersService.isTokenMember(userDetails, memberId);
        CustomPageDTO<PostDTO> customPageDTO = postServiceClient.getBookmarkedPost(userId, postType, boardIds, page - 1, size, sort);

        //TODO N+1 개선 필요 -> 개선됨
        customPageDTO.getContent().forEach(it -> {
            postUtil.addAuthorToPost(it);
            postUtil.addFilePost(it);
        });

        return ResponseDTO.createSuccessResponse("success", customPageDTO);
    }

    /**
     * 좋아요 토글
     */
    @Operation(summary = "좋아요 토글", description = "게시글 좋아요를 토글합니다(좋아요/좋아요 취소).")
    @PostMapping("/likes")
    public ResponseDTO<String> toggleLike(
            @Parameter(description = "게시글 ID") @RequestParam("postId") String postId,
            @Parameter(description = "사용자 ID") @RequestParam("userId") String userId) {
        postServiceClient.toggleLike(postId, userId);
        return ResponseDTO.createSuccessResponse("success", "");
    }

    @Operation(summary = "좋아요한 게시물 조회", description = "사용자가 좋아요한 게시글 목록을 조회합니다.")
    @GetMapping("/like/list")
    public ResponseDTO<CustomPageDTO<PostDTO>> getLikedPost(
            @Parameter(description = "사용자 ID") @RequestParam("userId") String userId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 방식") @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

//        Members member = membersService.isTokenMember(userDetails, memberId);
        CustomPageDTO<PostDTO> customPageDTO = postServiceClient.getLikedPost(userId, page, size, sort);

        //TODO N+1 개선 필요 -> 개선됨
        customPageDTO.getContent().forEach(it -> {
            postUtil.addAuthorToPost(it);
            postUtil.addFilePost(it);
        });

        // 신고 및 차단 정보 설정
//        if (userDetails != null && StringUtils.hasText(userDetails.getUserId())) {
//            reportUtils.setReportAndBlockInfo(customPageDTO.getContent(), userDetails.getUserId());
//        }

        return ResponseDTO.createSuccessResponse("success", customPageDTO);
    }

    @Operation(summary = "유저의 댓글이 달린 게시물 조회", description = "유저의 댓글이 달린 게시물 조회합니다.")
    @GetMapping("/comments/posts")
    public ResponseDTO<CustomPageDTO<PostDTO>> getPostsContainsUserComment(
            @Parameter(description = "사용자 ID") @RequestParam("userId") String userId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 방식") @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CustomPageDTO<PostDTO> customPageDTO = postServiceClient.getPostsContainsUserComment(userId, page, size, sort);

        customPageDTO.getContent().forEach(it -> {
            postUtil.addAuthorToPost(it);
            postUtil.addFilePost(it);
        });

        return ResponseDTO.createSuccessResponse("success", customPageDTO);
    }

    @Operation(summary = "유저가 작성한 댓글 및 게시글 카운트 조회", description = "유저가 작성한 댓글 및 게시글 카운트를 조회합니다.")
    @GetMapping("/counts")
    public ResponseDTO<PostCountDTO> getPostCountsByUserId(
            @RequestParam("userId") String userId
    ){
        PostCountDTO postCountDTO = postServiceClient.getPostCountsByUserId(userId);
        return ResponseDTO.createSuccessResponse("게시글/댓글 수 조회 성공", postCountDTO);
    }

    /**
     * 좋아요 목록 조회
     */
    @Operation(summary = "좋아요 목록 조회", description = "게시글의 좋아요 목록을 조회합니다.")
    @GetMapping("/likes/{postId}")
    public ResponseDTO<List<UserDTO>> getLikes(
            @Parameter(description = "게시글 ID") @PathVariable String postId) {
        List<String> likedUserIds = postServiceClient.getLikes(postId);
        List<UserDTO> memberInfo = authService.getMemberInfoList(likedUserIds);
        return ResponseDTO.createSuccessResponse("success", memberInfo);
    }


    /**
     * 북마크 토글
     */
    @Operation(summary = "북마크 토글", description = "게시글 북마크를 토글합니다(북마크/북마크 취소).")
    @PostMapping("/bookmarks/toggle")
    public ResponseDTO<String> toggleBookmark(
            @Parameter(description = "게시글 ID") @RequestParam("postId") String postId,
            @Parameter(description = "사용자 ID") @RequestParam("userId") String userId) {
        postServiceClient.toggleBookmark(postId, userId);
        return ResponseDTO.createSuccessResponse("success", "");
    }

    /**
     * 조회수 증가
     */
    @Operation(summary = "조회수 증가", description = "게시글 조회수를 증가시킵니다. 1시간 쿨타임이 적용됩니다.")
    @PostMapping("/{postId}/views")
    public ResponseDTO<PostViewResponseDTO> increaseViewCount(
            @Parameter(description = "게시글 ID") @PathVariable("postId") String postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseDTO.createSuccessResponse("success",
                    PostViewResponseDTO.error("로그인이 필요합니다."));
        }

        String userId = userDetails.getUserId();

        try {
            boolean increased = postViewService.increaseViewCount(postId, userId);

            if (increased) {
                return ResponseDTO.createSuccessResponse("success", PostViewResponseDTO.success());
            } else {
                long remainingMinutes = postViewService.getRemainingCooldownMinutes(userId, postId);
                return ResponseDTO.createSuccessResponse("success",
                        PostViewResponseDTO.cooldown(remainingMinutes));
            }

        } catch (Exception e) {
            return ResponseDTO.createSuccessResponse("success",
                    PostViewResponseDTO.error("조회수 증가 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 댓글 등록
     */
    @Operation(summary = "댓글 등록", description = "게시글에 새 댓글을 등록합니다.")
    @PostMapping("/comments")
    public ResponseDTO<String> createComment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "댓글 생성 정보")
            @RequestBody CreateCommentDTO createCommentDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
//        Members member = membersService.isTokenMember(userDetails, createCommentDTO.getAuthorId());
        createCommentDTO.setAuthorId(userDetails.getUserId());
        CommentDTO commentDTO = postServiceClient.createComment(createCommentDTO.getPostId(), createCommentDTO);

        // ===== 새소식, 푸시알림 ===== //
        PostDTO postDTO = postServiceClient.getPostById(commentDTO.getPostId());
        return ResponseDTO.createSuccessResponse("success", "");
    }

    /**
     * 댓글 목록 조회
     */
    @Operation(summary = "댓글 목록 조회", description = "게시글의 댓글 목록을 조회합니다.")
    @GetMapping("/comments/{postId}")
    public ResponseDTO<List<CommentDTO<UserDTO>>> getComments(
            @Parameter(description = "게시글 ID") @PathVariable(value = "postId") String postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<CommentDTO<UserDTO>> comments = postServiceClient.getComments(postId);

        //TODO N+1 개선 필요 -> 개선됨
        comments.forEach(it -> {
            postUtil.addAuthorToComment(it);
        });


        return ResponseDTO.createSuccessResponse("success", comments);
    }

    /**
     * 댓글 개수 조회
     */
    @Operation(summary = "댓글 개수 조회", description = "게시글의 댓글 개수를 조회합니다.")
    @GetMapping("/comments/count/{postId}")
    public ResponseDTO<Integer> getCommentsCount(
            @Parameter(description = "게시글 ID") @PathVariable String postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Integer commentsCount = postServiceClient.getCommentsCount(postId);
        return ResponseDTO.createSuccessResponse("success", commentsCount);
    }

    /**
     * 댓글 조회
     */
    @Operation(summary = "댓글 상세 조회", description = "특정 댓글을 조회합니다.")
    @GetMapping("/comments/{postId}/{commentId}")
    public ResponseDTO<CommentDTO<UserDTO>> getComment(
            @Parameter(description = "게시글 ID") @PathVariable String postId,
            @Parameter(description = "댓글 ID") @PathVariable String commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CommentDTO<UserDTO> comment = postServiceClient.getComment(postId, commentId);
        return ResponseDTO.createSuccessResponse("success", comment);
    }

    /**
     * 댓글 삭제
     */
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
    @DeleteMapping("/comments/{postId}/{commentId}")
    public ResponseDTO<String> deleteComment(
            @Parameter(description = "게시글 ID") @PathVariable String postId,
            @Parameter(description = "댓글 ID") @PathVariable String commentId,
            @Parameter(description = "작성자 ID") @RequestParam("authorId") String authorId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        //권한 확인
//        membersService.isTokenMember(userDetails.getUsername(), authorId);

        CommentDTO comment = postServiceClient.getComment(postId, commentId);

        //본인글 확인
        boolean equals = comment.getAuthorId().equals(authorId);
        if (equals) {
            String commentsCount = postServiceClient.deleteComments(postId, commentId);
            return ResponseDTO.createSuccessResponse("success", commentsCount);
        }

        return ResponseDTO.createSuccessResponse("권한 없음", null);
    }

    /**
     * 댓글 수정
     */
    @Operation(summary = "댓글 수정", description = "댓글을 수정합니다.")
    @PutMapping("/comments")
    public ResponseDTO<String> updateComment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "댓글 수정 정보")
            @RequestBody UpdateCommentDTO updateCommentDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        //권한 확인
//        membersService.isTokenMember(userDetails.getUsername(), updateCommentDTO.getAuthorId());

        String comment = postServiceClient.updateComments(updateCommentDTO.getPostId(), updateCommentDTO.getId(), updateCommentDTO);
        return ResponseDTO.createSuccessResponse("success", comment);
    }

    /**
     * 게시판 목록 조회
     */
    @Operation(summary = "게시판 목록 조회", description = "게시판 목록을 조회합니다.")
    @GetMapping("/board")
    public ResponseDTO<List<BoardDTO>> getBoardList(
            @Parameter(description = "카테고리 ID") @RequestParam(value = "categoryId", required = false) Long categoryId,
            @Parameter(description = "카테고리 고유 키") @RequestParam(value = "categoryUniqueKey", required = false) String categoryUniqueKey) {

        System.out.println(categoryId);
        List<BoardDTO> boardList = postServiceClient.getBoardList(categoryId, categoryUniqueKey);
        return ResponseDTO.createSuccessResponse("success", boardList);
    }

    /**
     * 카테고리 생성
     */
    @Operation(summary = "카테고리 생성", description = "새 카테고리를 생성합니다.")
    @PostMapping("/category")
    public ResponseDTO<Long> createCategory(
            @Parameter(description = "회원 ID") @RequestParam("member_id") String memberId,
            @Parameter(description = "카테고리 이름") @RequestParam("categoryName") String categoryName,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

//        Members member = membersService.isTokenMember(userDetails, memberId);
//        if(member.getGrade() != 1) {
//            throw new CustomException("ERROR", ErrorCode.ERROR_AUTH);
//        }

        Long category = postServiceClient.createCategory("JAKDANG", categoryName);
        return ResponseDTO.createSuccessResponse("success", category);
    }

    /**
     * 카테고리 목록 조회
     */
    @Operation(summary = "카테고리 목록 조회", description = "카테고리 목록을 조회합니다.")
    @GetMapping("/category")
    public ResponseDTO<List<PostCategoryDTO>> getCategoryList(
            @Parameter(description = "소유자 ID") @RequestParam(value = "ownerId", required = false) String ownerId) {

        List<PostCategoryDTO> categoryList = postServiceClient.getCategoryList(ownerId);
        return ResponseDTO.createSuccessResponse("success", categoryList);
    }

    /**
     * 카테고리 수정
     */
    @Operation(summary = "카테고리 수정", description = "카테고리 정보를 수정합니다.")
    @PatchMapping("/category")
    public ResponseDTO<List<PostCategoryDTO>> updateCategory(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "카테고리 수정 정보")
            @RequestBody PostCategoryDTO postCategoryDTO,
            @Parameter(description = "회원 ID") @RequestParam("member_id") String memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

//        Members member = membersService.isTokenMember(userDetails, memberId);
//        if(member.getGrade() != 1) {
//            throw new CustomException("ERROR", ErrorCode.ERROR_AUTH);
//        }

        List<PostCategoryDTO> categoryList = postServiceClient.updateCategory(postCategoryDTO);
        return ResponseDTO.createSuccessResponse("success", categoryList);
    }

    /**
     * 카테고리 삭제
     */
    @Operation(summary = "카테고리 삭제", description = "카테고리를 삭제합니다.")
    @DeleteMapping("/category/{id}")
    public ResponseDTO<Long> deleteCategory(
            @Parameter(description = "카테고리 ID") @PathVariable Long id,
            @Parameter(description = "회원 ID") @RequestParam("member_id") String memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

//        Members member = membersService.isTokenMember(userDetails, memberId);
//        if(member.getGrade() != 1) {
//            throw new CustomException("ERROR", ErrorCode.ERROR_AUTH);
//        }

        Long deletedId = postServiceClient.deleteCategory(id, "JAKDANG");
        return ResponseDTO.createSuccessResponse("success", deletedId);
    }

    /**
     * 게시판 생성
     */
    @Operation(summary = "게시판 생성", description = "새 게시판을 생성합니다.")
    @PostMapping("/board")
    public ResponseDTO<Long> createBoard(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "게시판 생성 정보")
            @RequestBody BoardDTO boardDTO,
            @Parameter(description = "회원 ID") @RequestParam("member_id") String memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

//        Members member = membersService.isTokenMember(userDetails, memberId);
//        if(member.getGrade() != 1) {
//            throw new CustomException("ERROR", ErrorCode.ERROR_AUTH);
//        }

        boardDTO.setOwnerId("JAKDANG");
        Long boardId = postServiceClient.createBoard(boardDTO);
        return ResponseDTO.createSuccessResponse("success", boardId);
    }

    /**
     * 게시판 수정
     */
    @Operation(summary = "게시판 수정", description = "게시판 정보를 수정합니다.")
    @PatchMapping("/board")
    public ResponseDTO<Long> updateBoard(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "게시판 수정 정보")
            @RequestBody BoardDTO boardDTO,
            @Parameter(description = "회원 ID") @RequestParam("member_id") String memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

//        Members member = membersService.isTokenMember(userDetails, memberId);
//        if(member.getGrade() != 1) {
//            throw new CustomException("ERROR", ErrorCode.ERROR_AUTH);
//        }

        boardDTO.setOwnerId("JAKDANG");
        Long successId = postServiceClient.updateBoard(boardDTO);
        return ResponseDTO.createSuccessResponse("success", successId);
    }

    /**
     * 게시판 삭제
     */
    @Operation(summary = "게시판 삭제", description = "게시판을 삭제합니다.")
    @DeleteMapping("/board/{id}")
    public ResponseDTO<Long> deleteBoard(
            @Parameter(description = "게시판 ID") @PathVariable Long id,
            @Parameter(description = "회원 ID") @RequestParam("member_id") String memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

//        Members member = membersService.isTokenMember(userDetails, memberId);
//        if(member.getGrade() != 1) {
//            throw new CustomException("ERROR", ErrorCode.ERROR_AUTH);
//        }

        Long deletedId = postServiceClient.deleteBoard(id, "JAKDANG");
        return ResponseDTO.createSuccessResponse("success", deletedId);
    }

    /**
     * 게시글 검색 (검색 보조 테이블 활용)
     */
    @Operation(summary = "게시글 검색", description = "제목, 내용, 작성자명으로 게시글을 검색합니다.")
    @GetMapping("/search")
    public ResponseDTO<CustomPageDTO<PostDTO>> searchPosts(
            @Parameter(description = "검색어") @RequestParam("keyword") String keyword,
            @Parameter(description = "검색 타입 (title: 제목, content: 내용, author: 작성자, all: 전체)")
            @RequestParam(value = "searchType", defaultValue = "all") String searchType,
            @Parameter(description = "커뮤니티 ID (범위 지정)") @RequestParam(value = "communityId", required = false) String communityId,
            @Parameter(description = "게시판 ID (단일 게시판 검색)") @RequestParam(value = "boardId", required = false) String boardId,
            @Parameter(description = "게시판 ID 목록 (여러 게시판 동시 검색)") @RequestParam(value = "boardIds", required = false) List<String> boardIds,
            @Parameter(description = "페이지 번호") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (keyword == null || keyword.trim().isEmpty()) {
            CustomPageDTO<PostDTO> emptyResult = new CustomPageDTO<>();
            emptyResult.setContent(new ArrayList<>());
            emptyResult.setTotalElements(0);
            emptyResult.setTotalPages(0);
            emptyResult.setNumber(0);
            emptyResult.setLast(true);
            return ResponseDTO.createSuccessResponse("검색어가 필요합니다.", emptyResult);
        }

        try {
            CustomPageDTO<PostDTO> searchResults = postSearchService.searchPosts(
                    keyword.trim(), searchType, communityId, boardId, boardIds, page, size);

            // 추가 정보 보강
            searchResults.getContent().forEach(post -> {
                postUtil.addAuthorToPost(post);
                postUtil.addFilePost(post);
                postUtil.addTargetClass(post);
            });

            return ResponseDTO.createSuccessResponse("검색 완료", searchResults);

        } catch (Exception e) {
            log.error("게시글 검색 중 오류 발생", e);
            CustomPageDTO<PostDTO> emptyResult = new CustomPageDTO<>();
            emptyResult.setContent(new ArrayList<>());
            emptyResult.setTotalElements(0);
            emptyResult.setTotalPages(0);
            emptyResult.setNumber(0);
            emptyResult.setLast(true);
            return ResponseDTO.createSuccessResponse("검색 중 오류가 발생했습니다.", emptyResult);
        }
    }

    @Operation(summary = "게시물 차단 상태 변경", description = "게시물의 차단 상태를 변경합니다.")
    @PatchMapping("/{id}/block")
    public ResponseDTO<PostDTO> updatePostBlockStatus(
            @Parameter(description = "게시글 ID") @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "차단 상태 정보")
            @RequestBody PostBlockDTO blockDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 관리자 권한 체크
        if (userDetails.getRole() != RoleType.ROLE_ADMIN) {
            throw new CustomException("관리자만 접근 가능합니다.", -400);
        }

        PostDTO updatedPost = postServiceClient.updatePostBlockStatus(id, blockDTO);
        return ResponseDTO.createSuccessResponse("success", updatedPost);
    }
}