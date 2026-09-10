package ounce.market.demo.common.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ounce.market.demo.common.service.S3UploadService;

import java.io.IOException;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    // todo 리뷰 사진도 s3UploadService 이거로 하면 될듯

    private final S3UploadService s3UploadService;

    // 프론트엔드에서 사진을 보내면 S3에 올리고 URL을 반환하는 API
    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = s3UploadService.uploadImage(file);
            return ResponseEntity.ok(imageUrl);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("이미지 업로드에 실패했습니다.");
        }
    }
}