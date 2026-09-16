package ounce.market.demo.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ounce.market.demo.common.service.ImageUrlResolver;
import ounce.market.demo.common.service.S3UploadService;
import ounce.market.demo.product.dto.request.ProductCreateRequest;
import ounce.market.demo.product.dto.request.ProductSearchCondition;
import ounce.market.demo.product.dto.response.ProductSliceResponse;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.entity.ProductCategory;
import ounce.market.demo.product.entity.ProductStatus;
import ounce.market.demo.product.repository.CategoryRepository;
import ounce.market.demo.product.repository.ProductCategoryRepository;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.product.dto.response.ProductResponse;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final S3UploadService s3UploadService;
    private final ImageUrlResolver imageUrlResolver;


    // 상품 상세
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        return ProductResponse.from(product, imageUrlResolver);
    }


    @Transactional
    public Long createProduct(ProductCreateRequest request, MultipartFile image) throws IOException {

//        log.info("image = {}, size = {}",
//                image == null ? "NULL" : image.getOriginalFilename(),
//                image == null ? -1 : image.getSize());

        String uploadedImageUrl = null;

        if (image != null && !image.isEmpty()) {
            uploadedImageUrl = s3UploadService.uploadImage(image);
        }

        // 💡 1. 여기서 변수 이름을 분리합니다!
        Product newProduct = request.toEntity();

        if (uploadedImageUrl != null) {
            newProduct.updateImageUrl(uploadedImageUrl);
        }

        // 💡 2. DB에 저장한 결과는 savedProduct 라는 '새로운 변수'에 담습니다.
        Product savedProduct = productRepository.save(newProduct);

        categoryRepository.findAllById(request.getCategoryIds())
                .forEach(c -> productCategoryRepository.save(
                        ProductCategory.builder()
                                .product(savedProduct)
                                .category(c)
                                .build()
                ));

        return savedProduct.getProductId();
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductCreateRequest request,
                                         MultipartFile image) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. productId=" + productId));

        product.updateDetails(
                request.getName(),
                request.getBasePrice(),
                request.getDiscountPercent(),
                request.getDescription(),
                request.getStock()
        );

        if (image != null && !image.isEmpty()) {
            product.updateImageUrl(s3UploadService.uploadImage(image));
        }

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            productCategoryRepository.deleteAllByProduct_ProductId(productId);
            categoryRepository.findAllById(request.getCategoryIds())
                    .forEach(category -> productCategoryRepository.save(
                            ProductCategory.builder()
                                    .product(product)
                                    .category(category)
                                    .build()
                    ));
        }

        return ProductResponse.from(product, imageUrlResolver);
    }

    @Transactional
    public void deleteProduct(Long productId){

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. productId = " + productId));



        product.changeStatus(ProductStatus.STOPPED);
    }


}
