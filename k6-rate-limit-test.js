import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        user_a_spam: {
            executor: 'constant-arrival-rate',
            rate: 20, // 20 requests per second (Exceeds USER limit of 10)
            timeUnit: '1s',
            duration: '10s',
            preAllocatedVUs: 10,
            maxVUs: 50,
            exec: 'spamRequest',
        },
        user_b_normal: {
            executor: 'constant-arrival-rate',
            rate: 2, // 2 requests per second (Well within USER limit)
            timeUnit: '1s',
            duration: '10s',
            preAllocatedVUs: 2,
            maxVUs: 10,
            exec: 'normalRequest',
        },
    },
};

// You need to replace these tokens with actual valid JWT tokens from your running app
const USER_A_TOKEN = 'YOUR_USER_A_JWT_TOKEN_HERE';
const USER_B_TOKEN = 'YOUR_USER_B_JWT_TOKEN_HERE';

const BASE_URL = 'http://localhost:8080/api/seats/1/hold-status'; // Using a GET endpoint to test rate limit first

export function spamRequest() {
    let res = http.get(BASE_URL, {
        headers: {
            'Authorization': `Bearer ${USER_A_TOKEN}`
        }
    });

    check(res, {
        'User A - status is 200 or 429': (r) => r.status === 200 || r.status === 429,
        'User A - hit rate limit (429)': (r) => r.status === 429,
    });
}

export function normalRequest() {
    let res = http.get(BASE_URL, {
        headers: {
            'Authorization': `Bearer ${USER_B_TOKEN}`
        }
    });

    check(res, {
        'User B - status is 200': (r) => r.status === 200,
    });
}
