package com.jakdang.labs.api.post.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 추가: 처리되지 않은 필드 무시
public class CustomPageDTO<T> {
    private List<T> content;
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
    private PageableDTO pageable;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageableDTO {
//        private List<SortDTO> sort;
        private int pageNumber;
        private int pageSize;
        private long offset;
        private boolean paged;
        private boolean unpaged;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class SortDTO {
            private boolean sorted;
            private boolean unsorted;
            private boolean empty;
        }
    }
}
