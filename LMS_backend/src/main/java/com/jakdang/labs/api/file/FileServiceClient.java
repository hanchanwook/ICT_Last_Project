package com.jakdang.labs.api.file;


import com.jakdang.labs.api.common.ResponseDTO;
import com.jakdang.labs.api.file.dto.*;
import com.jakdang.labs.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(name = "file-service", url = "${file-service.url}", configuration = FeignConfig.FeignErrorDecoder.class)
public interface FileServiceClient {

    //TODO 파일 붙이는중

    @PostMapping("/download")
    byte[] downloadFile(@RequestBody RequestFileDTO dto);

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseDTO<Object> handleFileUpload(@RequestPart("file") MultipartFile file,
                                         @RequestParam(value = "index", required = false) Integer index,
                                         @RequestParam(value = "width", required = false) Integer width,
                                         @RequestParam(value = "height", required = false) Integer height,
                                         @RequestParam(value = "ownerId", required = false) String ownerId,
                                         @RequestParam(value = "memberType", required = false) MemberEnum memberType) ;

    @GetMapping("/upload/{fileName}")
    ResponseDTO<FileInfoDTO> generatePresignedUrlForUpload(@PathVariable("fileName") String fileName);

    @PostMapping("/upload/success")
    ResponseDTO<ResponseFileDTO> handleUploadSuccess(@RequestBody RequestFileDTO requestFileDTO, @RequestParam(required = false, value = "ownerId") String ownerId,
                                                     @RequestParam(required = false, value = "memberType") MemberEnum memberType);

    @PostMapping(value = "/upload-audio", consumes = "multipart/form-data")
    ResponseDTO<ResponseFileDTO> uploadAudio(@RequestParam("file") MultipartFile file,
                                             @RequestParam("ownerId") String ownerId,
                                             @RequestParam("memberType") MemberEnum memberType);

    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    ResponseDTO<ResponseFileDTO> uploadImage(@RequestParam("file") MultipartFile file,
                                             @RequestParam("ownerId") String ownerId,
                                             @RequestParam("memberType") MemberEnum memberType);

    @PostMapping(value = "/upload-file", consumes = "multipart/form-data")
    ResponseDTO<ResponseFileDTO> uploadFile(@RequestParam("file") MultipartFile file,
                                            @RequestParam("ownerId") String ownerId,
                                            @RequestParam("memberType") MemberEnum memberType);

    @GetMapping("/imagelink/{fileId}")
    ResponseDTO<String> getImageLink(@PathVariable("fileId") String fileId);

    @GetMapping("/image/{fileId}")
    byte[] getImage(@PathVariable("fileId") String fileId);

    @GetMapping("/image/{fileId}/{size}")
    byte[] getImageResize(@PathVariable("fileId") String fileId, @PathVariable("size") int size);

    @PostMapping("/image/resources/{imageName}/{size}")
    byte[] getPublicImageResize(@PathVariable("imageName") String imageName, @PathVariable("size") int size);

    @GetMapping("/thumbnail/{fileId}")
    byte[] getThumbnail(@PathVariable("fileId") String fileId);

    @GetMapping("/image/member/{memberId}")
    byte[] getMemberImage(@PathVariable("memberId") String memberId);

    @GetMapping("/image/user/{userId}")
    byte[] getUserImage(@PathVariable("userId") String userId);

    @GetMapping("/image/school/{schoolId}")
    byte[] getSchoolImage(@PathVariable("schoolId") String schoolId);

    @GetMapping("/image/kid/{kidId}")
    byte[] getKidImage(@PathVariable("kidId") String kidId);

    @GetMapping("/image/media/{mediaId}")
    byte[] getImageAlbumMedia(@PathVariable("mediaId") String mediaId);

    @GetMapping("/image/kid_all/{kidId}")
    List<FileOwnerDTO> getAllKidImage(@PathVariable("kidId") String kidId);

    @DeleteMapping("/{fileId}")
    ResponseDTO<?> deleteFile(@PathVariable("fileId") String fileId);

    @PostMapping("/findAll")
     ResponseDTO<List<ResponseFileDTO>> findAllById(@RequestBody List<String> fileIds);
}
