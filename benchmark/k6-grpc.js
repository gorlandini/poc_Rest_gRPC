import grpc from 'k6/net/grpc';
import { check, sleep } from 'k6';

// Requer build customizado do k6 com a extensão xk6-grpc:
//   xk6 build --with github.com/grafana/xk6-grpc
// Execução: k6 run --vus 100 --duration 60s k6-grpc.js

const client = new grpc.Client();
client.load(['../grpc-produtos-api/src/main/proto'], 'produto.proto');

const ADDR = __ENV.GRPC_ADDR || 'localhost:9090';

export const options = {
  scenarios: {
    carga_constante: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS) || 100,
      duration: __ENV.DURATION || '60s',
    },
  },
  thresholds: {
    grpc_req_duration: ['p(95)<500'],
  },
};

export default () => {
  client.connect(ADDR, { plaintext: true });

  const listResponse = client.invoke(
    'portfolio.produtos.v1.ProdutoService/ListarProdutos',
    { pagina: 0, tamanho: 20 }
  );
  check(listResponse, { 'list status OK': (r) => r && r.status === grpc.StatusOK });

  const createResponse = client.invoke(
    'portfolio.produtos.v1.ProdutoService/CriarProduto',
    {
      nome: 'Produto Benchmark',
      descricao: 'Produto criado durante o teste de carga',
      preco: 99.9,
      quantidade_estoque: 10,
    }
  );
  check(createResponse, { 'create status OK': (r) => r && r.status === grpc.StatusOK });

  client.close();
  sleep(0.1);
};
