//package ounce.market.demo.order.service;
//
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.springframework.data.redis.core.RedisTemplate;
//import ounce.market.demo.cart.entity.CartProduct;
//import ounce.market.demo.cart.repository.CartProductRepository;
//import ounce.market.demo.cart.repository.CartRepository; // 추가!
//import ounce.market.demo.delivery.entity.DeliveryType;
//import ounce.market.demo.member.entity.Member;
//import ounce.market.demo.member.repository.MemberRepository;
//import ounce.market.demo.order.dto.request.OrderCreateRequest;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import ounce.market.demo.product.entity.Product;
//import ounce.market.demo.product.repository.ProductRepository;
//import ounce.market.demo.product.repository.StockRedisRepository;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.atomic.AtomicInteger;
//
//@SpringBootTest(properties = {
//        "spring.datasource.url=jdbc:mysql://localhost:3306/Ounce",
//        "spring.datasource.username=root",
//        "spring.datasource.password=Mickey51799@@",
//        "JWT_SECRET=thisisverylongtestsecretkeyforouncemarketproject2026!",
//})
//class OrderServiceConcurrencyTest {
//
//    @Autowired private OrderService orderService;
//    @Autowired private MemberRepository memberRepository;
//    @Autowired private ProductRepository productRepository;
//    @Autowired private CartProductRepository cartProductRepository;
//    @Autowired private CartRepository cartRepository; // 🚨 추가된 장바구니 리포지토리
//    @Autowired private StockRedisRepository stockRedisRepository;
//    @Autowired private RedisTemplate<String, String> redisTemplate; // ✅ 추가 1: Redis 정리/검증용
//
//
//    // 생성된 유저와 장바구니 상품의 진짜 ID를 담아둘 리스트
//    private List<Member> savedMembers = new ArrayList<>();
//    private List<CartProduct> savedCartProducts = new ArrayList<>();
//    private Product savedProduct; // 생성된 진짜 상품
//
//    private static final int STOCK = 10; // ✅ 재고 상수화 (DB와 Redis를 같은 값으로)
//
//
//    @BeforeEach
//    void setUp() {
//        // 1. 자식 테이블부터 순서대로 깔끔하게 비우기
//        cartProductRepository.deleteAll();
//        cartRepository.deleteAll();
//        memberRepository.deleteAll();
//        productRepository.deleteAll();
//
//        // 리스트 초기화 (테스트 간 간섭 방지)
//        savedMembers.clear();
//        savedCartProducts.clear();
//
//        // 2. 타임딜 상품 세팅
//        Product product = Product.builder()
//                .productCode("TIME-DEAL-001")
//                .name("제철 밤 10시 타임딜")
//                .basePrice(1000L)
//                .stock(STOCK)
//                .description("테스트용 타임딜")
//                .imageUrl("https://test.com/img.jpg")
//                .build();
//        savedProduct = productRepository.save(product); // 진짜 저장된 상품 정보 기억하기!
//
//        // Redis 재고 세팅 (진짜 부여된 상품 ID 사용)
//        stockRedisRepository.setStock(savedProduct.getProductId(), 10);
//
//        // 3. 유저 100명 및 장바구니 세팅
//        for (int i = 1; i <= 100; i++) {
//            Member member = Member.builder()
//                    .email("test" + i + "@ounce.com")
//                    .password("12345678")
//                    .name("타임딜러" + i)
//                    .point(1000000)
//                    .build();
//            Member savedMember = memberRepository.save(member);
//            savedMembers.add(savedMember); // 진짜 저장된 유저 정보 기억하기!
//
//            // 💡 주의: Member 엔티티 쪽에 연관된 Cart 생성이 필요하다면 여기서 Cart를 생성/저장하는 로직이 추가되어야 할 수도 있습니다.
//            // (일단 기존 혜준님 코드 흐름대로 CartProduct만 만듭니다)
//            CartProduct cartProduct = CartProduct.builder()
//                    // .member(savedMember) // Member나 Cart 맵핑이 필요하다면 주석 풀기
//                    .product(savedProduct)
//                    .quantity(1)
//                    .build();
//            CartProduct savedCartProduct = cartProductRepository.save(cartProduct);
//            savedCartProducts.add(savedCartProduct); // 진짜 저장된 장바구니 상품 기억하기!
//        }
//    }
//
//    @AfterEach
//    void tearDown() {
//        redisTemplate.delete("product:stock:" + savedProduct.getProductId());
//    }
//
//    @Test
//    @DisplayName("타임딜 동시성 제어: 100명의 유저가 동시에 타임딜 상품을 결제하면, 재고 수량(10개)만큼만 성공하고 나머지는 실패해야 한다.")
//    void timeDeal_concurrent_order_test() throws InterruptedException {
//        int threadCount = 100;
//        ExecutorService executorService = Executors.newFixedThreadPool(32);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//
//        AtomicInteger successCount = new AtomicInteger();
//        AtomicInteger failCount = new AtomicInteger();
//
//        // when
//        for (int i = 0; i < threadCount; i++) {
//            // 🚨 하드코딩(1, 2, 3...) 대신, 진짜 DB에 저장된 ID를 꺼내옵니다!!
//            Long memberId = savedMembers.get(i).getMemberId();
//            Long cartProductId = savedCartProducts.get(i).getCartProductId();
//
//            OrderCreateRequest request = new OrderCreateRequest(
//                    List.of(cartProductId),
//                    DeliveryType.NORMAL
//            );
//
//            executorService.submit(() -> {
//                try {
//                    orderService.createOrderFromCart(memberId, request);
//                    successCount.incrementAndGet();
//                } catch (Exception e) {
//                    failCount.incrementAndGet();
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await();
//        executorService.shutdown();
//
//        // then
//        assertThat(successCount.get()).isEqualTo(10);
//        assertThat(failCount.get()).isEqualTo(90);
//
//        Product finalProduct = productRepository.findById(savedProduct.getProductId()).orElseThrow();
//        System.out.println("DB 최종 재고 = " + finalProduct.getStock());
//
//        String remainingStock = redisTemplate.opsForValue().get("product:stock:" + savedProduct.getProductId());
//        System.out.println("Redis 최종 재고 = " + remainingStock);
//
//        assertThat(Integer.parseInt(remainingStock)).isEqualTo(0);
//        assertThat(finalProduct.getStock()).isEqualTo(0);
//    }
//}