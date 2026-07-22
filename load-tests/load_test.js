import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        critical_p0_traffic: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 20 },
                { duration: '20s', target: 20 },
                { duration: '10s', target: 0 },
            ],
            exec: 'p0Traffic',
        },
        background_p2_traffic: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 80 },
                { duration: '20s', target: 80 },
                { duration: '10s', target: 0 },
            ],
            exec: 'p2Traffic',
        },
    },
    thresholds: {
        // We expect P0 to have almost 100% success rate even under load
        'http_req_failed{scenario:critical_p0_traffic}': ['rate<0.05'], 
        // We expect P2 to be heavily shed, so we don't put a strict success threshold on it
    },
};

const BASE_URL = 'http://adaptive-api-gateway:8080/api/v1/orders'; // Docker Gateway route

export function p0Traffic() {
    const payload = JSON.stringify({
        customerId: "CUST-K6-" + __VU,
        restaurantId: "REST-K6",
        items: "Burger"
    });
    
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-Priority-Tier': 'P0',
        },
    };

    const res = http.post(BASE_URL, payload, params);
    check(res, {
        'P0 status is 200': (r) => r.status === 200,
    });
    sleep(1);
}

export function p2Traffic() {
    const params = {
        headers: {
            'X-Priority-Tier': 'P2',
        },
    };

    const res = http.get(BASE_URL, params);
    check(res, {
        'P2 status is 200': (r) => r.status === 200,
        'P2 status is 429 (Shed)': (r) => r.status === 429,
    });
    sleep(1);
}
