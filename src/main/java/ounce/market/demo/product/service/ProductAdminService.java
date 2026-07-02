//package ounce.market.demo.product.service;
//
//import lombok.RequiredArgsConstructor;
//import ounce.market.demo.product.dto.ProductRegisterCommand;
//import ounce.market.demo.product.entity.Product;
//import ounce.market.demo.product.entity.ProductStatus;
//import ounce.market.demo.product.entity.Stock;
//import ounce.market.demo.product.repository.ProductRepository;
//import ounce.market.demo.product.repository.StockRepository;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//// 관리자가 상품을 수동으로 등록하는 단계 (MVP 기준 정책)
//@Service
//@RequiredArgsConstructor
//public class ProductAdminService {
//
//    private final ProductRepository productRepository;
//    private final StockRepository stockRepository;
//
//    @Transactional
//    public Long registerProduct(ProductRegisterCommand command) {
//        Product product = Product.builder()
//                .productCode(command.productCode())
//                .name(command.name())
//                .basePrice(command.basePrice())
//                .description(command.description())
//                .imageUrl(command.imageUrl())
//                .build();
//        productRepository.save(product);
//
//        Stock stock = Stock.builder()
//                .quantity(command.initialStock())
//                .product(product)
//                .build();
//        stockRepository.save(stock);
//
//        return product.getProductId();
//    }
//
//    // PREPARING -> ON_SALE: 관리자가 등록 내용을 확인하고 고객 화면에 노출시키는 단계
//    @Transactional
//    public void activateProduct(Long productId) {
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. productId=" + productId));
//        product.changeStatus(ProductStatus.ON_SALE);
//    }
//}
