package com.jakdang.labs.api.post.model;

import lombok.Data;

@Data
public class UpdateCommentDTO {
    private String id;                 // 댓글 ID
    private String postId;             // 관련 게시물 ID
    private String authorId;           // 작성자 ID
    private String content;            // 댓글 내용
}
