package com.jakdang.labs.api.post.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
public class PostSearchCondition {
    private String ownerId;
    private List<String> targetIds;
    private boolean published;
    private String postType;
    private String authorId;
    private String search;
    @Builder.Default
    private List<Long> boardIds = new ArrayList<>();
    @Builder.Default
    private Boolean includeNull = true; // null 포함 여부를 명시적으로 결정
    private String includeTargetPrefix; // targetIds에 해당 글자가 포함된 게시물도 같이 조회
    private String date; // null 포함 여부를 명시적으로 결정
    private String channelId; // 채널 ID 필터링용 필드 추가
}
