package com.jakdang.labs.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileExceptionCode implements ErrorType {
    TEST_EXCEPTION_CODE("파일 시스템 테스트 익스셉션", 401),
    NOT_FOUND_FILE("해당 파일을 찾을 수 없습니다.", 400),
    NOT_FOUND_IMAGE("해당 이미지를 찾을 수 없습니다.", 400),
    NOT_FOUND_INFO("해당 이미지/파일/비디오의 데이터를 찾을 수 없습니다", 500),
    FAILED_CREATED_THUMB("썸네일 생성에 실패했습니다.", 500),
    FAILED_CAPTURE_VIDEO_SCREEN("동영상 썸네일을 촬영하는데 실패했습니다.", 500),
    FAILED_GET_VIDEO_DURATION("동영상 길이를 가져오는데 실패했습니다.", 500),
    FAILED_UPLOAD_THUMB_S3("AWS S3에 썸네일을 업로드 하는데 실패했습니다.", 500),
    FAILED_DELETE_LOCAL_FILE("로컬에 저장된 파일 삭제에 실패했습니다.", 500),
    AWS_S3GET_ERROR("AWS S3에서 파일을 가져오는데 실패했습니다.", 400),
    AWS_S3POST_ERROR("AWS S3에 POST(업로드)를 실패했습니다.", 500),
    EMPTY_FILE("AWS S3에서 가져온 파일이 빈 파일입니다.", 400),
    DIRECTORY_CREATE_ERROR("파일 디렉터리 생성 실패", 500),
    FAILED_COPY("파일 복사를 실패했습니다.", 500),
    WRONG_TYPE_FILE("잘못된 파일입니다.", 500);

    private final String message;
    private final int status;
}
