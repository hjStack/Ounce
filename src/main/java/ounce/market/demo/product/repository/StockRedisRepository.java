package ounce.market.demo.product.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StockRedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String STOCK_KEY_PREFIX = "product:stock:";

    private static final RedisScript<Long> DECREASE_SCRIPT = RedisScript.of("""
            local stock = tonumber(redis.call('GET', KEYS[1]))
            if stock == nil then return -2 end
            if stock < tonumber(ARGV[1]) then return -1 end
            return redis.call('DECRBY', KEYS[1], ARGV[1])
            """, Long.class);

    private String key(Long productId) {
        return STOCK_KEY_PREFIX + productId;
    }

    /**  관리자가 재입고 등으로 의도적으로 값을 확정할 때 쓰는 용도로 남겨둠 */
    public void setStock(Long productId, int stock) {
        redisTemplate.opsForValue().set(key(productId), String.valueOf(stock));
    }

    /** 재고 초기화/재입고 시 호출 */
    public void initStockIfAbsent(Long productId, int stock) {
        redisTemplate.opsForValue().setIfAbsent(key(productId), String.valueOf(stock));
    }

    /** 원자적 차감. 성공 시 남은 재고, 실패 시 예외 */
    public long decrease(Long productId, int quantity) {
        Long result = redisTemplate.execute(
                DECREASE_SCRIPT, List.of(key(productId)), String.valueOf(quantity));

        if (result == null || result == -2) {
            throw new IllegalStateException("재고 정보가 초기화되지 않았습니다. (productId=" + productId + ")");
        }
        if (result == -1) {
            throw new IllegalArgumentException("재고가 부족합니다. (productId=" + productId + ")");
        }
        return result;
    }

    /** 보상: DB 실패 시 깎았던 재고 되돌리기 */
    public void increase(Long productId, int quantity) {
        redisTemplate.opsForValue().increment(key(productId), quantity);
    }
}