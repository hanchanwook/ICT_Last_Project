package com.jakdang.labs.api.post.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BaseResponse<T> {
    private int resultCode;
    private String message;
    @JsonProperty("data")
    private T data;


    // CustomPageDTO 관련 필드
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("size")
    private Integer size;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("total_count")
    private Long totalCount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("total_page")
    private Integer totalPage;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("page")
    private Integer page;


    public BaseResponse(int resultCode, String message, T data) {
        this.resultCode = resultCode;
        this.message = message;
        this.data = data;
    }

    // CustomPageDTO 응답 생성
    public static <T> BaseResponse<T> successWithPage(CustomPageDTO<T> pageDTO) {
        BaseResponse<T> response = (BaseResponse<T>) new BaseResponse<>(200, "Success", pageDTO.getContent());
        response.size = pageDTO.getSize();
        response.totalCount = pageDTO.getTotalElements();
        response.totalPage = pageDTO.getTotalPages();
        response.page = pageDTO.getNumber() + 1;
        return response;
    }

    // 성공 응답 생성
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(200, "Success", data);
    }

    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>(200, message, data);
    }

    // 에러 응답 생성
    public static <T> BaseResponse<T> error(int resultCode, String message) {
        return new BaseResponse<>(resultCode, message, null);
    }

    public static <T> BaseResponse<T> error(String message) {
        return new BaseResponse<>(-1, message, null);
    }
}
