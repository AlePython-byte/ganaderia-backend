import http from 'k6/http';
import crypto from 'k6/crypto';
import { check } from 'k6';

const REQUIRED_ENV = ['BASE_URL', 'DEVICE_TOKEN', 'DEVICE_SECRET'];

for (const name of REQUIRED_ENV) {
  if (!__ENV[name] || __ENV[name].trim() === '') {
    throw new Error(`${name} is required. Pass it with -e ${name}=<VALUE> or an environment variable.`);
  }
}

const baseUrl = __ENV.BASE_URL.replace(/\/+$/, '');
const deviceToken = __ENV.DEVICE_TOKEN;
const deviceSecret = __ENV.DEVICE_SECRET;
const endpointPath = '/api/device/locations';
const duration = __ENV.DURATION || '30s';
const vus = positiveInteger(__ENV.VUS, 1);
const requestsPerSecond = nonNegativeInteger(__ENV.REQUESTS_PER_SECOND, 0);

const latitude = numericEnv('LATITUDE', 1.2136);
const longitude = numericEnv('LONGITUDE', -77.2811);
const batteryLevel = numericEnv('BATTERY_LEVEL', 85);
const gpsAccuracy = numericEnv('GPS_ACCURACY', 8.5);

export const options = {
  scenarios: requestsPerSecond > 0
    ? {
        controlled_rate: {
          executor: 'constant-arrival-rate',
          rate: requestsPerSecond,
          timeUnit: '1s',
          duration,
          preAllocatedVUs: Math.max(vus, 1),
          maxVUs: Math.max(vus * 2, 2),
        },
      }
    : {
        smoke: {
          executor: 'constant-vus',
          vus,
          duration,
        },
      },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  const headerTimestamp = utcHeaderTimestamp();
  const bodyTimestamp = localBodyTimestampMinusOneMinute();
  const nonce = uniqueNonce();
  const rawBody = JSON.stringify({
    latitude,
    longitude,
    timestamp: bodyTimestamp,
    batteryLevel,
    gpsAccuracy,
  });

  const canonicalRequest = `POST\n${endpointPath}\n${headerTimestamp}\n${nonce}\n${rawBody}`;
  const signature = crypto.hmac('sha256', deviceSecret, canonicalRequest, 'base64');

  const response = http.post(`${baseUrl}${endpointPath}`, rawBody, {
    headers: {
      'Content-Type': 'application/json',
      'X-Device-Token': deviceToken,
      'X-Device-Timestamp': headerTimestamp,
      'X-Device-Nonce': nonce,
      'X-Device-Signature': signature,
    },
  });

  check(response, {
    'device ingestion returns HTTP 200': (res) => res.status === 200,
    'device ingestion latency stays under 5 seconds': (res) => res.timings.duration < 5000,
  });
}

function numericEnv(name, fallback) {
  const raw = __ENV[name];
  if (raw === undefined || raw === null || raw.trim() === '') {
    return fallback;
  }

  const parsed = Number(raw);
  if (Number.isNaN(parsed)) {
    throw new Error(`${name} must be numeric.`);
  }
  return parsed;
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

function nonNegativeInteger(raw, fallback) {
  if (raw === undefined || raw === null || String(raw).trim() === '') {
    return fallback;
  }

  const parsed = Number(raw);
  if (!Number.isInteger(parsed) || parsed < 0) {
    throw new Error('REQUESTS_PER_SECOND must be a non-negative integer.');
  }
  return parsed;
}

function utcHeaderTimestamp() {
  return new Date().toISOString().replace(/\.\d{3}Z$/, 'Z');
}

function localBodyTimestampMinusOneMinute() {
  const date = new Date(Date.now() - 60_000);
  return [
    date.getFullYear(),
    pad2(date.getMonth() + 1),
    pad2(date.getDate()),
  ].join('-') + `T${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`;
}

function uniqueNonce() {
  return `${__VU}-${__ITER}-${Date.now()}-${randomSuffix()}`;
}

function randomSuffix() {
  return Math.random().toString(36).slice(2, 12);
}

function pad2(value) {
  return String(value).padStart(2, '0');
}
