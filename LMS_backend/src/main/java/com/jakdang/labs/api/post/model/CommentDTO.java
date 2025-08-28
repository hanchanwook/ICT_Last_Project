package com.jakdang.labs.api.post.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommentDTO<T> {
    private String id;                 // 댓글 ID
    private String postId;             // 관련 게시물 ID
    private String authorId;           // 작성자 ID
    private String content;            // 댓글 내용
    private String parentId;           // 부모 댓글 ID (대댓글인 경우 사용)
    private String parentAuthorId;           // 부모 댓글 AuthorId (대댓글인 경우 사용)
    private String rootCommentId ;           // 최초 댓글 ID (대댓글인 경우 사용)
    private int depth ;           // 최초 댓글 ID (대댓글인 경우 사용)
    private String createdAt;   // 생성 시간
    private String updatedAt;   // 수정 시간
    private boolean deleted;   // 삭제여부
    private boolean blocked;   // 신고차단여부
    private List<CommentDTO<T>> childComments = new ArrayList<>();  // 대댓글 리스트

    private T author;           // 작성자
    private T parentAuthor;           // 작성자
    private boolean isReport;
    private boolean isBlockedUser;

}
