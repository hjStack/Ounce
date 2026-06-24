//package ounce.market.demo.order.service;
//
//import ounce.market.demo.member.entity.Member;
//import ounce.market.demo.member.entity.Role;
//import ounce.market.demo.member.repository.MemberRepository;
//import ounce.market.demo.product.dto.ProductRegisterCommand;
//import ounce.market.demo.product.entity.Stock;
//import ounce.market.demo.product.repository.StockRepository;
//import ounce.market.demo.product.service.ProductAdminService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//// 밤 10시 정각, 재고보다 훨씬 많은 사람이 동시에 "구매" 버튼을 눌렀을 때
//// 정확히 재고 수만큼만 성공하고 나머지는 품절로 튕겨나가는지 증명하는 테스트.
//@SpringBootTest
//class OrderConcurrencyTest {
//
//    @Autowired
//    private ProductAdminService productAdminService;
//    @Autowired
//    private OrderFacade orderFacade;
//    @Autowired
//    private MemberRepository memberRepository;
//    @Autowired
//    private StockRepository stockRepository;
//
//    @Test
//    void 동시에_재고보다_많은_요청이_들어와도_정확히_재고수만큼만_판매된다() throws InterruptedException {
//        // given: 재고 10개짜리 상품 + 충분한 포인트를 가진 회원 30명
//        int stockQuantity = 10;
//        int requesterCount = 30;
//
//        Long productId = productAdminService.registerProduct(new ProductRegisterCommand(
//                "TEST-" + System.nanoTime(), "동시성 테스트 상품", 10_000, "설명", null, stockQuantity));
//        productAdminService.activateProduct(productId);
//
//        List<Long> memberIds = new ArrayList<>();
//        for (int i = 0; i < requesterCount; i++) {
//            Member member = Member.builder()
//                    .password("pw")
//                    .role(Role.USER)
//                    .point(1_000_000)
//                    .build();
//            memberRepository.save(member);
//            memberIds.add(member.getMemberId());
//        }
//
//        ExecutorService executor = Executors.newFixedThreadPool(requesterCount);
//        CountDownLatch latch = new CountDownLatch(requesterCount);
//        AtomicInteger successCount = new AtomicInteger();
//        AtomicInteger failCount = new AtomicInteger();
//
//        // when: 모든 회원이 거의 동시에 같은 상품 1개씩 구매 시도
//        for (Long memberId : memberIds) {
//            executor.submit(() -> {
//                try {
//                    orderFacade.purchase(memberId, productId, 1);
//                    successCount.incrementAndGet();
//                } catch (Exception e) {
//                    failCount.incrementAndGet();
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//        latch.await();
//        executor.shutdown();
//
//        // then: 재고 수만큼만 성공하고, 실제 남은 재고는 0 (오버셀 없음)
//        Stock stock = stockRepository.findByProduct_ProductId(productId).orElseThrow();
//        assertEquals(stockQuantity, successCount.get());
//        assertEquals(requesterCount - stockQuantity, failCount.get());
//        assertEquals(0, stock.getQuantity());
//    }
//}
