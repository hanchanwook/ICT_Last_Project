package com.jakdang.labs.api.post.model;

import lombok.Data;

@Data
public class CreateCommentDTO {
    private String postId;
    private String content;
    private String authorId;
    private String parentId;
}

