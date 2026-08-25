import http from 'k6/http';
import { check } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const contentId = __ENV.CONTENT_ID || '1';
const rps = Number(__ENV.RPS || '5');

export const options = {
  scenarios: {
    route_verification: {
      executor: 'constant-arrival-rate',
      rate: rps,
      timeUnit: '1s',
      duration: __ENV.DURATION || '3m',
      preAllocatedVUs: Number(__ENV.PRE_VUS || '20'),
      maxVUs: Number(__ENV.MAX_VUS || '100'),
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<5000'],
  },
};

export default function () {
  const response = http.get(`${baseUrl}/api/contents/${contentId}/route-verification`, {
    tags: { endpoint: 'route-verification', cache_mode: __ENV.CACHE_MODE || 'baseline' },
  });
  check(response, { 'status is 2xx': (r) => r.status >= 200 && r.status < 300 });
}
