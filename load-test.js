
import http from 'k6/http';
import { check } from 'k6';

export let options = {
    stages: [
        { duration: '10s', target: 100 }, // 10초 동안 100명까지 증가
        { duration: '20s', target: 400 }, // 20초 동안 400명으로 훅 증가 (한계 돌파)
        { duration: '10s', target: 0 },   // 다시 0으로 감소
    ],
};

export default function () {
    // ⚠️ 여기에 실제 Ounce 서버의 주소를 적어주세요!
    let res = http.post(`https://ouncefresh.com/api/test/delay?ts=${new Date().getTime()}`);
    check(res, {
        'status is 200': (r) => r.status === 200,
    });
}