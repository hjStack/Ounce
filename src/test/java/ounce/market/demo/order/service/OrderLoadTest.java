//package ounce.market.demo.order.service;
//
//import ounce.market.demo.member.entity.Member;
//import ounce.market.demo.member.entity.Role;
//import ounce.market.demo.member.repository.MemberRepository;
//import ounce.market.demo.product.dto.ProductRegisterCommand;
//import ounce.market.demo.product.service.ProductAdminService;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.ValueSource;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//// 평소 mvn test에서는 건너뛰고, 동시 요청 규모를 키워가며 한계점을 찾을 때만 명시적으로 실행:
//// ./mvnw test -Dtest=OrderLoadTest -Djunit.jupiter.conditions.deactivate='org.junit.*DisabledCondition'
//@Disabled("수동 부하테스트 - 평소 빌드에서는 제외")
//@SpringBootTest
//class OrderLoadTest {
//
//    private static final int STOCK_QUANTITY = 100;
//
//    @Autowired
//    private ProductAdminService productAdminService;
//    @Autowired
//    private OrderFacade orderFacade;
//    @Autowired
//    private MemberRepository memberRepository;
//
//    @ParameterizedTest(name = "동시 요청 {0}명 vs 재고 " + STOCK_QUANTITY + "개")
//    @ValueSource(ints = {10000, 30000, 100000})
//    void 동시요청_규모별_한계점_탐색(int requesterCount) throws InterruptedException {
//        Long productId = productAdminService.registerProduct(new ProductRegisterCommand(
//                "LOAD-" + System.nanoTime(), "부하테스트 상품", 10_000, "설명", null, STOCK_QUANTITY));
//        productAdminService.activateProduct(productId);
//
//        List<Long> memberIds = new ArrayList<>();
//        for (int i = 0; i < requesterCount; i++) {
//            Member member = Member.builder().password("pw").role(Role.USER).point(1_000_000).build();
//            memberRepository.save(member);
//            memberIds.add(member.getMemberId());
//        }
//
//        AtomicInteger successCount = new AtomicInteger();
//        AtomicInteger failCount = new AtomicInteger();
//        Map<String, AtomicInteger> failureByType = new ConcurrentHashMap<>();
//        CountDownLatch latch = new CountDownLatch(requesterCount);
//
//        long start = System.nanoTime();
//        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
//            for (Long memberId : memberIds) {
//                executor.submit(() -> {
//                    try {
//                        orderFacade.purchase(memberId, productId, 1);
//                        successCount.incrementAndGet();
//                    } catch (Exception e) {
//                        failCount.incrementAndGet();
//                        failureByType.computeIfAbsent(e.getClass().getSimpleName(), k -> new AtomicInteger())
//                                .incrementAndGet();
//                    } finally {
//                        latch.countDown();
//                    }
//                });
//            }
//            latch.await();
//        }
//        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
//
//        System.out.printf("[LOAD] requesters=%d success=%d fail=%d elapsedMs=%d failureByType=%s%n",
//                requesterCount, successCount.get(), failCount.get(), elapsedMs, failureByType);
//
//        assertEquals(STOCK_QUANTITY, successCount.get(), "오버셀/언더셀 없이 정확히 재고만큼만 성공해야 함");
//    }
//}
