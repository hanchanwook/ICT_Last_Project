package com.jakdang.labs.api.post.model;

import lombok.Data;

import java.util.List;

@Data
public class PostCreateDTO {
    private String id;
    private String title;
    private String contents;
    private String postType;
    private Long boardId;
    private String authorId;
    private String ownerId; // schoolService의 id, communityService의 id 등 사용
    private List<String> targetIds; // arrowed_class 등
    private List<String> targetKidIds; // 태그 대상 공개

    private List<String> fileIds; // 리스트로 파일 ID들을 저장
    private List<String> addFileIds;
    private List<String> deleteFileIds;

    private boolean published = false; //임시저장, 게시 여부
    private boolean important = false;
    private boolean blocked = false;
    private String startDate;
    private String endDate;
}
