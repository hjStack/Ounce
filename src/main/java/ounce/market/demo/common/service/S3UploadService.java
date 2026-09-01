
package ounce.market.demo.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    /*
    todo cloudFront 붙이기
    key에 "img/  " 가 붙고
    확장자를 contentType으로 정하고

     */

    private static final String IMAGE_PREFIX = "img/";

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final S3Client s3Client;

    // cloudFront 붙이면 이거 빼기
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("파일이 비어있습니다.");

        String contentType = file.getContentType();
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("JPEG, PNG, WEBP 이미지만 업로드할 수 있습니다.");
        }

        String key = IMAGE_PREFIX + UUID.randomUUID() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        // 공식 SDK를 사용한 업로드
        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // 업로드된 파일의 접속 URL 조합하여 반환
        return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + key;
    }
}