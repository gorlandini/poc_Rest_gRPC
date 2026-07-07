# REST vs gRPC — Comparativo com Spring Boot

Dois microsserviços Spring Boot que implementam **exatamente a mesma regra de negócio**
(um cadastro simples de Produtos), expostos por dois protocolos diferentes:

| Projeto | Protocolo | Porta | Serialização |
|---|---|---|---|
| `rest-produtos-api` | REST/HTTP 1.1 | `8080` | JSON (Jackson) |
| `grpc-produtos-api` | gRPC/HTTP 2 | `9090` (gRPC) / `8081` (actuator) | Protobuf |

O objetivo é isolar o overhead de **transporte e serialização**, não o de lógica de negócio:
os dois projetos usam a mesma camada de `Service`/`Repository` (em memória, sem banco de
dados), variando apenas a "casca" — `@RestController` de um lado, `@GrpcService` do outro.

## Estrutura

```
portfolio-benchmark/
├── rest-produtos-api/      # Spring Boot + Spring Web
├── grpc-produtos-api/      # Spring Boot + grpc-spring-boot-starter (net.devh)
└── benchmark/              # Scripts k6 para reproduzir os testes de carga
    ├── k6-rest.js
    └── k6-grpc.js
```

## Como rodar

### REST
```bash
cd rest-produtos-api
./mvnw spring-boot:run
# API em http://localhost:8080/api/v1/produtos
```

### gRPC
```bash
cd grpc-produtos-api
./mvnw spring-boot:run
# gRPC em localhost:9090 (use grpcurl ou BloomRPC/Postman para testar)
```

Exemplo com `grpcurl`:
```bash
grpcurl -plaintext -d '{"pagina": 0, "tamanho": 20}' \
  localhost:9090 portfolio.produtos.v1.ProdutoService/ListarProdutos
```

## Endpoints / RPCs equivalentes

| Ação | REST | gRPC |
|---|---|---|
| Criar | `POST /api/v1/produtos` | `ProdutoService/CriarProduto` |
| Buscar por id | `GET /api/v1/produtos/{id}` | `ProdutoService/BuscarProduto` |
| Listar (paginado) | `GET /api/v1/produtos?pagina=&tamanho=` | `ProdutoService/ListarProdutos` |
| Deletar | `DELETE /api/v1/produtos/{id}` | `ProdutoService/DeletarProduto` |

---

## Benchmark

### Metodologia

- **Ferramenta:** [k6](https://k6.io) com a extensão [xk6-grpc](https://github.com/grafana/xk6-grpc) para ambos os protocolos (mesma ferramenta nos dois lados, para não introduzir viés de medição)
- **Ambiente:** _(preencher: specs da máquina — CPU, RAM, SO)_, serviços executados um de cada vez (não simultaneamente)
- **Warm-up:** 15s de aquecimento (JIT) descartados antes da coleta
- **Duração da coleta:** 60s por cenário
- **Payload:** objeto `Produto` com 5 campos (nome, descrição, preço, estoque, timestamp)
- **Cenários de carga:** 10, 50, 100 e 500 usuários virtuais (VUs) concorrentes
- **Operações medidas:** `GET/List` (listagem paginada de 20 itens) e `POST/Create`
- **Data da execução:** _(preencher)_

### Resultados

> Rode os comandos da seção [Reproduzindo o benchmark](#reproduzindo-o-benchmark) e cole a
> saída do `k6` (ou os valores extraídos dela) nas tabelas abaixo.

#### Latência — operação de listagem (20 itens), por carga concorrente

| VUs | REST p50 | REST p95 | REST p99 | gRPC p50 | gRPC p95 | gRPC p99 |
|---|---|---|---|---|---|---|
| 10  | | | | | | |
| 50  | | | | | | |
| 100 | | | | | | |
| 500 | | | | | | |

#### Throughput sustentado (requisições/segundo)

| VUs | REST (req/s) | gRPC (req/s) | Diferença |
|---|---|---|---|
| 10  | | | |
| 50  | | | |
| 100 | | | |
| 500 | | | |

#### Payload e uso de recursos

| Métrica | REST/JSON | gRPC/Protobuf |
|---|---|---|
| Tamanho médio da resposta (list, 20 itens) | | |
| CPU média sob 100 VUs | | |
| Memória heap média sob 100 VUs | | |

### Leitura dos resultados

_(preencher após rodar: pontos onde gRPC/REST se destacaram, onde a diferença foi maior/menor
que o esperado, comportamento sob alta concorrência, etc.)_

### Reproduzindo o benchmark

```bash
# REST
k6 run --env VUS=100 --env DURATION=60s benchmark/k6-rest.js

# gRPC (requer build do k6 com xk6-grpc)
k6 run --env VUS=100 --env DURATION=60s benchmark/k6-grpc.js
```

---

## Tecnologias

- Java 17
- Spring Boot 3.3
- Spring Web (REST)
- grpc-spring-boot-starter / net.devh (gRPC)
- Protocol Buffers 3
- k6 + xk6-grpc (testes de carga)

## Possíveis evoluções

- Adicionar persistência real (Postgres) para medir o impacto de I/O de banco
- Expor métricas via Micrometer + Prometheus/Grafana para observar os testes em tempo real
- Adicionar streaming gRPC (server-streaming) como terceiro cenário de comparação
