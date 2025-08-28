package com.jakdang.labs.api.post.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jakdang.labs.api.auth.dto.UserDTO;
import com.jakdang.labs.api.file.dto.ResponseFileDTO;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@ToString
public class PostDTO {
    private String id;
    private String title;
    private String contents;
    private String postType;
    private String author_id;
    private String owner_id; // schoolService의 id, communityService의 id 등 사용
    private List<String> target_ids; // schoolService의 id, communityService의 id 등 사용
    private String published_time;
    private String updated_at;
    private String created_at;
    private String end_date;
    private Long boardId;
    private List<String> file_ids;
    private int comment_count;
    private int like_count;
    private int view_count;
    @JsonProperty("is_modified")
    private boolean is_modified = false;
    @JsonProperty("is_like")
    private boolean is_like = false;
    @JsonProperty("is_bookmark")
    private boolean is_bookmark = false;
    private boolean important = false;
    @JsonProperty("is_published")
    private boolean is_published = false;
    @JsonProperty("is_blocked")
    private boolean is_blocked = false;
    private boolean welcome= false;
    @JsonProperty("is_report")
    private boolean is_report = false;
    private boolean isBlockedUser = false;
    // ========글쓴이 정보==============//
    private UserDTO author;
    private List<ResponseFileDTO> files;
//    private List<ClassVO> classList;
    // ==============================//

}

