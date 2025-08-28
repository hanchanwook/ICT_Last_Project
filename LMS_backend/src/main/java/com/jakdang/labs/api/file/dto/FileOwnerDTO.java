package com.jakdang.labs.api.file.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileOwnerDTO {

    private String id;
    private String owner;
    private String fileId;
    private MemberEnum memberType;
}
