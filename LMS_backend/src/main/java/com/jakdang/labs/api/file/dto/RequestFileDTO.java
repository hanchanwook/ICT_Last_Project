package com.jakdang.labs.api.file.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestFileDTO {

    private String id;
    private String name;
    private FileEnum type;

    private String key;
    private String address;

    @JsonProperty("thumbnail_key")
    private String thumbnailKey;

    @JsonProperty("active")
    private boolean isActive;

    @JsonProperty("thumbnail_address")
    private String thumbnailAddress;

    private int width;
    private int height;

    private int index;

    private double duration;

    private Long size;

    @Override
    public String toString() {
        return "RequestFileDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", key='" + key + '\'' +
                ", address='" + address + '\'' +
                ", thumbnailKey='" + thumbnailKey + '\'' +
                ", isActive=" + isActive +
                ", thumbnailAddress='" + thumbnailAddress + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", index=" + index +
                ", duration=" + duration +
                ", size=" + size +
                '}';
    }
}
