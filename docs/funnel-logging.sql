-- 운영 DB에서 ddl-auto=validate를 사용하는 환경은 아래 DDL을 먼저 적용한다.
CREATE TABLE events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    user_id BIGINT NULL,
    event_type VARCHAR(40) NOT NULL,
    metadata JSON NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_events_session_created_at (session_id, created_at),
    INDEX idx_events_type_created_at (event_type, created_at)
);

-- 날짜별 퍼널 모수: 세션 기준으로 중복 이벤트를 제거한다.
SELECT DATE(created_at) AS event_date,
       COUNT(DISTINCT CASE WHEN event_type = 'page_view' THEN session_id END) AS page_views,
       COUNT(DISTINCT CASE WHEN event_type = 'signup' THEN session_id END) AS signups,
       COUNT(DISTINCT CASE WHEN event_type = 'product_view' THEN session_id END) AS product_views,
       COUNT(DISTINCT CASE WHEN event_type = 'cart_add' THEN session_id END) AS cart_adds,
       COUNT(DISTINCT CASE WHEN event_type = 'order_complete' THEN session_id END) AS order_completes
FROM events
WHERE created_at >= CURRENT_DATE - INTERVAL 30 DAY
GROUP BY DATE(created_at)
ORDER BY event_date DESC;

-- 단계별 전환율(최근 하루). 분모가 0이면 NULL을 반환한다.
WITH daily AS (
    SELECT
        COUNT(DISTINCT CASE WHEN event_type = 'page_view' THEN session_id END) AS page_views,
        COUNT(DISTINCT CASE WHEN event_type = 'signup' THEN session_id END) AS signups,
        COUNT(DISTINCT CASE WHEN event_type = 'product_view' THEN session_id END) AS product_views,
        COUNT(DISTINCT CASE WHEN event_type = 'cart_add' THEN session_id END) AS cart_adds,
        COUNT(DISTINCT CASE WHEN event_type = 'order_complete' THEN session_id END) AS order_completes
    FROM events
    WHERE created_at >= CURRENT_DATE
)
SELECT page_views, signups, product_views, cart_adds, order_completes,
       100.0 * signups / NULLIF(page_views, 0) AS page_to_signup_rate,
       100.0 * product_views / NULLIF(signups, 0) AS signup_to_product_rate,
       100.0 * cart_adds / NULLIF(product_views, 0) AS product_to_cart_rate,
       100.0 * order_completes / NULLIF(cart_adds, 0) AS cart_to_order_rate,
       100.0 * order_completes / NULLIF(page_views, 0) AS overall_conversion_rate
FROM daily;
