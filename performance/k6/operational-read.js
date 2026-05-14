import http from 'k6/http';
import { check, group, sleep } from 'k6';

const REQUIRED_ENV = ['BASE_URL', 'JWT'];

for (const name of REQUIRED_ENV) {
  if (!__ENV[name] || __ENV[name].trim() === '') {
    throw new Error(`${name} is required. Pass it with -e ${name}=<VALUE> or an environment variable.`);
  }
}

const baseUrl = __ENV.BASE_URL.replace(/\/+$/, '');
const jwt = __ENV.JWT;
const vus = positiveInteger(__ENV.VUS, 2);
const duration = __ENV.DURATION || '30s';
const includeAdminOutbox = booleanEnv('INCLUDE_ADMIN_OUTBOX');
const includeAiSummary = booleanEnv('INCLUDE_AI_SUMMARY');

export const options = {
  scenarios: {
    operational_read_smoke: {
      executor: 'constant-vus',
      vus,
      duration,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<700'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  group('cows', () => {
    getAndCheck('/api/cows/page?sort=id&direction=ASC&page=0&size=10', 'cows page');
  });

  group('collars', () => {
    getAndCheck('/api/collars/page?sort=id&direction=ASC&page=0&size=10', 'collars page');
  });

  group('alerts', () => {
    getAndCheck('/api/alerts', 'alerts list');
    getAndCheck('/api/alerts/status/PENDIENTE', 'pending alerts by status');
    getAndCheck('/api/alerts/pending/priority-queue', 'pending alerts priority queue');
  });

  group('dashboard', () => {
    getAndCheck('/api/dashboard/summary', 'dashboard summary');
  });

  group('alert-analysis', () => {
    getAndCheck('/api/alert-analysis/summary', 'alert analysis summary');
    getAndCheck('/api/alert-analysis/top-priorities?limit=5', 'alert analysis top priorities');
  });

  if (includeAdminOutbox) {
    group('admin-outbox', () => {
      getAndCheck('/api/admin/notification-outbox?channel=EMAIL&page=0&size=10', 'admin notification outbox');
    });
  }

  if (includeAiSummary) {
    group('ai-summary', () => {
      getAndCheck('/api/alert-analysis/ai-summary', 'alert analysis ai summary');
    });
  }

  sleep(1);
}

function getAndCheck(path, label) {
  const requestId = uniqueRequestId(label);
  const response = http.get(`${baseUrl}${path}`, {
    headers: {
      Authorization: `Bearer ${jwt}`,
      'Content-Type': 'application/json',
      'X-Request-Id': requestId,
    },
    tags: {
      endpoint: label,
    },
  });

  check(response, {
    [`${label} returns HTTP 200`]: (res) => res.status === 200,
    [`${label} does not return 401`]: (res) => res.status !== 401,
    [`${label} does not return 403`]: (res) => res.status !== 403,
    [`${label} does not return 500`]: (res) => res.status < 500,
  });
}

function positiveInteger(raw, fallback) {
  if (raw === undefined || raw === null || String(raw).trim() === '') {
    return fallback;
  }

  const parsed = Number(raw);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error('VUS must be a positive integer.');
  }
  return parsed;
}

function booleanEnv(name) {
  const raw = __ENV[name];
  if (raw === undefined || raw === null || String(raw).trim() === '') {
    return false;
  }

  return ['true', '1', 'yes', 'y'].includes(String(raw).trim().toLowerCase());
}

function uniqueRequestId(label) {
  const normalizedLabel = label.replace(/[^A-Za-z0-9._-]+/g, '-');
  return `k6-operational-read-${normalizedLabel}-${__VU}-${__ITER}-${Date.now()}`;
}
