package ounce.market.demo.common.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ounce.market.demo.common.service.S3UploadService;

import java.io.IOException;

@Tag(name = "16-2. 관리자 사진 등록", description = " 관리자 사진 등록 API")
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    // 관리자가 상품 등록할때 이미지 올리는 컨트롤러
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