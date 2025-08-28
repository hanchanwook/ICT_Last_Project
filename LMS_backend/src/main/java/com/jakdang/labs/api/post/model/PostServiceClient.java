package com.jakdang.labs.api.post.model;

import com.jakdang.labs.api.auth.dto.UserDTO;
import com.jakdang.labs.api.post.dto.PostCountDTO;
import com.jakdang.labs.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "post-service", url = "${post-service.url}", configuration = FeignConfig.FeignErrorDecoder.class)
public interface PostServiceClient {

    //게시물 1건 조회
    @GetMapping("/{id}")
    PostDTO getPostById(@PathVariable("id") String id);


    //게시물 1건 삭제
    @DeleteMapping("/{id}")
    String deletePostById(@PathVariable("id") String id);

    //글쓰기
    @PostMapping
    PostDTO createPost(@RequestBody PostCreateDTO postDTO);

    //업데이트
    @PutMapping("/{id}")
    PostDTO updatePost(@PathVariable("id") String id, @RequestBody PostCreateDTO postDTO);

    //게시물 리스트 조회
    @GetMapping
    CustomPageDTO<PostDTO> getPosts(
            @RequestBody PostSearchCondition searchCondition,
            @RequestParam(value = "userId", required = false) String userId, // 조회하는 사용자 ID
            @RequestParam(value = "page", required = false) int page,
            @RequestParam(value = "size", required = false) int size,
            @RequestParam(value = "sort", required = false) String sort
    );

    // 게시물 ID 목록으로 여러 게시물 조회
    @PostMapping("/list")
    List<PostDTO> getPostsByIds(
            @RequestBody List<String> postIds,
            @RequestParam(value = "userId", required = false) String userId
    );

    @GetMapping("/bookmarks")
    CustomPageDTO<PostDTO> getBookmarkedPost(
            @RequestParam(value = "userId", required = false) String userId, // 조회하는 사용자 ID
            @RequestParam(value = "postType", required = false) String postType, // 조회하는 사용자 ID
            @RequestParam(value = "boardIds", required = false) List<Long> boardIds, // 조회하는 사용자 ID
            @RequestParam(value = "page", required = false) int page,
            @RequestParam(value = "size", required = false) int size,
            @RequestParam(value = "sort", required = false) String sort
    );

    @GetMapping("/likes/list")
    CustomPageDTO<PostDTO> getLikedPost(
            @RequestParam(value = "userId", required = false) String userId, // 조회하는 사용자 ID
            @RequestParam(value = "page", required = false) int page,
            @RequestParam(value = "size", required = false) int size,
            @RequestParam(value = "sort", required = false) String sort
    );


    // 좋아요 토글
    @PostMapping("/{postId}/likes")
    void toggleLike(@PathVariable("postId") String postId, @RequestParam("userId") String userId);

    // 좋아요 목록 조회
    @GetMapping("/{postId}/likes")
    List<String> getLikes(@PathVariable("postId") String postId);

    // 북마크 토글
    @PostMapping("/{postId}/bookmarks")
    void toggleBookmark(@PathVariable("postId") String postId, @RequestParam("userId") String userId);

    // 조회수 증가
    @PostMapping("/{postId}/views")
    void increaseViewCount(@PathVariable("postId") String postId, @RequestParam("userId") String userId);

    // 댓글 작성
    @PostMapping("/{postId}/comments")
    CommentDTO createComment(@PathVariable("postId") String postId, @RequestBody CreateCommentDTO createCommentDTO);

    //댓글 리스트 조회
    @GetMapping("/{postId}/comments")
    List<CommentDTO<UserDTO>> getComments(@PathVariable("postId") String postId);

    //댓글 카운트
    @GetMapping("/{postId}/comments/count")
    Integer getCommentsCount(@PathVariable("postId") String postId);

    //댓글 1건 조회
    @GetMapping("/{postId}/comments/{commentId}")
    CommentDTO<UserDTO> getComment(@PathVariable("postId") String postId,
                           @PathVariable("commentId") String commentId);
    //댓글 삭제
    @DeleteMapping("/{postId}/comments/{commentId}")
    String deleteComments(@PathVariable("postId") String postId,
                          @PathVariable("commentId") String commentId);

    //댓글 수정
    @PutMapping("/{postId}/comments/{commentId}")
    String updateComments(
            @PathVariable("postId") String postId,
            @PathVariable("commentId") String commentId,
            UpdateCommentDTO updateCommentDTO);

    //카테고리 게시판 목록 가져오기
    @GetMapping("/board")
    List<BoardDTO> getBoardList(
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("categoryUniqueKey") String categoryUniqueKey
    );


    //카테고리 생성
    @PostMapping("/category")
    Long createCategory(
            @RequestParam(value = "ownerId", required = false) String ownerId, // 스쿨 아이디
            @RequestParam(value = "categoryName", required = false) String categoryName // 카테고리 이름
    );

    //카테고리 리스트 조회 OwnerId
    @GetMapping("/category")
    List<PostCategoryDTO> getCategoryList(
            @RequestParam(value = "ownerId", required = false) String ownerId // 스쿨 아이디
    );

    @PatchMapping("/category")
    List<PostCategoryDTO> updateCategory(
            @RequestBody PostCategoryDTO postCategoryDTO
    );

    @DeleteMapping("/category")
    Long deleteCategory(
            @RequestParam ("id") Long id,
            @RequestParam ("ownerId") String ownerId
    );


    //게시판 생성
    @PostMapping("/board")
    public Long createBoard(
            @RequestBody BoardDTO boardDTO
    );

    //게시판 삭제
    @DeleteMapping("/board")
    public Long deleteBoard(
            @RequestParam ("id") Long id,
            @RequestParam ("ownerId") String ownerId
    );

    //게시판 삭제
    @PatchMapping("/board")
    public Long updateBoard(
            @RequestBody BoardDTO boardDTO
    );

    // 게시물 차단 상태 변경
    @PatchMapping("/{id}/block")
    PostDTO updatePostBlockStatus(@PathVariable("id") String id, @RequestBody PostBlockDTO blockDTO);

    @GetMapping("/comments/posts")
    CustomPageDTO<PostDTO> getPostsContainsUserComment(
            @RequestParam("userId") String userId,
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sort") String sort);

    @GetMapping("/counts")
    PostCountDTO getPostCountsByUserId(@RequestParam("userId") String userId);
}