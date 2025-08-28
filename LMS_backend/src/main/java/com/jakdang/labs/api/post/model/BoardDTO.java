package com.jakdang.labs.api.post.model;
import lombok.Data;

@Data
public class BoardDTO {
    private Long id;
    private String name; // 게시판 이름
    private Long categoryId; // 카테고리 id
    private String ownerId;
    private Boolean active;
}
