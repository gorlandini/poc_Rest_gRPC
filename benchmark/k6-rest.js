import http from 'k6/http';
import { check, sleep } from 'k6';

// Execução: k6 run --vus 100 --duration 60s k6-rest.js
// Ajuste BASE_URL e VUs conforme o cenário do teste.

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    carga_constante: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS) || 100,
      duration: __ENV.DURATION || '60s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  // GET por id (usa um dos ids do seed - ajuste conforme necessário)
  const listRes = http.get(`${BASE_URL}/api/v1/produtos?pagina=0&tamanho=20`);
  check(listRes, { 'list status 200': (r) => r.status === 200 });

  const payload = JSON.stringify({
    nome: 'Produto Benchmark',
    descricao: 'Produto criado durante o teste de carga',
    preco: 99.9,
    quantidadeEstoque: 10,
  });

  const createRes = http.post(`${BASE_URL}/api/v1/produtos`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  check(createRes, { 'create status 201': (r) => r.status === 201 });

  sleep(0.1);
}
