-- 재고 충분하면 차감하고 남은 재고 반환, 부족하면 -1
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil then
    return -2            -- 재고 키 없음 (초기화 안 됨)
end
if stock < tonumber(ARGV[1]) then
    return -1            -- 재고 부족
end
return redis.call('DECRBY', KEYS[1], ARGV[1])