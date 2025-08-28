package com.jakdang.labs.api.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCountDTO {
    private long totalPosts;
    private long totalComments;
    private double postGrowthRate;
    private double commentGrowthRate;
}
