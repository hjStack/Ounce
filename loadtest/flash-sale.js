import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = 'https://ouncefresh.com';
const success = new Counter('order_success');  // 200 (차감 성공)
const soldout = new Counter('order_soldout');  // 409 (품절)

export const options = {
  scenarios: {
    flash_sale: {
      executor: 'per-vu-iterations',
      vus: 1000,        // 동시 유저 1000
      iterations: 1,    // 각자 1번씩
      maxDuration: '30s',
    },
  },
};

export default function () {
  const res = http.post(`${BASE}/api/test/stock-decrease`, null);

  if (res.status === 200) success.add(1);
  if (res.status === 409) soldout.add(1);

  check(res, {
    'success or soldout (no error)': (r) => r.status === 200 || r.status === 409,
  });
}
