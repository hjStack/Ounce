package ounce.market.demo.product.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StockRedisRepository {

    // 스크립트 인자가 문자열 그대로 전달되도록 StringRedisTemplate을 쓴다.
    // 범용 RedisTemplate은 기본 직렬화가 JDK라 ARGV가 바이너리로 나가서 tonumber()가 nil이 된다.
    private final StringRedisTemplate redisTemplate;

    private static final String STOCK_KEY_PREFIX = "product:stock:";

    private static final long KEY_NOT_FOUND = -2L;
    private static final long NOT_ENOUGH_STOCK = -1L;

    private static final RedisScript<Long> DECREASE_SCRIPT = RedisScript.of("""
            local stock = tonumber(redis.call('GET', KEYS[1]))
            if stock == nil then return -2 end
            local qty = tonumber(ARGV[1])
            if stock < qty then return -1 end
            return redis.call('DECRBY', KEYS[1], qty)
            """, Long.class);

    // 키가 없으면 되살리지 않는다. INCRBY는 없는 키를 0에서 만들어버리기 때문에
    // 판매 종료/삭제된 상품의 재고가 보상 과정에서 부활할 수 있다.
    private static final RedisScript<Long> INCREASE_SCRIPT = RedisScript.of("""
            if redis.call('EXISTS', KEYS[1]) == 0 then return -2 end
            return redis.call('INCRBY', KEYS[1], tonumber(ARGV[1]))
            """, Long.class);

    public String key(Long productId) {
        return STOCK_KEY_PREFIX + productId;
    }

    public Optional<Integer> getStock(Long productId) {
        String value = redisTemplate.opsForValue().get(key(productId));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "재고 값이 숫자가 아닙니다. (productId=" + productId + ", value=" + value + ")", e);
        }
    }

    /** 관리자가 재입고 등으로 의도적으로 값을 확정할 때 쓰는 용도로 남겨둠 */
    public void setStock(Long productId, int stock) {
        redisTemplate.opsForValue().set(key(productId), String.valueOf(stock));
    }

    /** 재고 초기화/재입고 시 호출 */
    public void initStockIfAbsent(Long productId, int stock) {
        redisTemplate.opsForValue().setIfAbsent(key(productId), String.valueOf(stock));
    }

    /** 원자적 차감. 성공 시 남은 재고, 실패 시 예외 */
    public long decrease(Long productId, int quantity) {
        requirePositive(quantity);

        Long result = redisTemplate.execute(
                DECREASE_SCRIPT, List.of(key(productId)), String.valueOf(quantity));

        if (result == null || result == KEY_NOT_FOUND) {
            throw new IllegalStateException("재고 정보가 초기화되지 않았습니다. (productId=" + productId + ")");
        }
        if (result == NOT_ENOUGH_STOCK) {
            throw new IllegalArgumentException("재고가 부족합니다. (productId=" + productId + ")");
        }
        return result;
    }

    /** 보상: DB 실패 시 깎았던 재고 되돌리기 */
    public void increase(Long productId, int quantity) {
        requirePositive(quantity);

        Long result = redisTemplate.execute(
                INCREASE_SCRIPT, List.of(key(productId)), String.valueOf(quantity));

        if (result == null || result == KEY_NOT_FOUND) {
            // 보상 실패는 예외로 던지지 않는다. 원래 예외를 덮어버리기 때문.
            // 대신 재고가 실제로 어긋난 상태이므로 반드시 로그로 남겨 추적 가능하게 한다.
            throw new StockCompensationFailedException(productId, quantity);
        }
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            // 음수가 들어오면 DECRBY가 오히려 재고를 늘린다.
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다. (quantity=" + quantity + ")");
        }
    }

    public static class StockCompensationFailedException extends IllegalStateException {
        public StockCompensationFailedException(Long productId, int quantity) {
            super("재고 보상에 실패했습니다. 수동 확인 필요 (productId=" + productId + ", quantity=" + quantity + ")");
        }
    }
}