package com.jakdang.labs.api.file.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Builder
public class ResponseFileDTO {

    private String id;
    private String name;
    private FileEnum type;
    private String key;
    private String address;

    @JsonProperty("thumbnail_address")
    private String thumbnailAddress;
    private int width;

    private int height;
    private int index;

    @JsonProperty("active")
    private boolean isActive;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    private double duration;
    private Long size;


}
