package com.jakdang.labs.api.post.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)// 사용 → 필요 없는 필드는 무시
public class PostCategoryDTO {
    private Long id;
    private String name; // 카테고리 이름
    private String uniqueKey;
    private String ownerId; // 어떤 school의 카테고리인가?
    private Boolean active;
    private List<BoardDTO> boards = new ArrayList<>(); // 해당 카테고리의 게시판
}
