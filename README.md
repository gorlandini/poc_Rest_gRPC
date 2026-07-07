# REST vs gRPC — Spring Boot Comparison

Two Spring Boot microservices that implement **the exact same business logic**
(a simple Product registry), exposed through two different protocols:

| Project | Protocol | Port | Serialization |
|---|---|---|---|
| `rest-produtos-api` | REST/HTTP 1.1 | `8080` | JSON (Jackson) |
| `grpc-produtos-api` | gRPC/HTTP 2 | `9090` (gRPC) / `8081` (actuator) | Protobuf |

The goal is to isolate the **transport and serialization** overhead, not the business
logic itself: both projects share the same `Service`/`Repository` layer (in-memory, no
database), differing only in the "shell" — a `@RestController` on one side, a
`@GrpcService` on the other.

## Structure

```
portfolio-benchmark/
├── rest-produtos-api/      # Spring Boot + Spring Web
├── grpc-produtos-api/      # Spring Boot + grpc-spring-boot-starter (net.devh)
└── benchmark/              # k6 scripts to reproduce the load tests
    ├── k6-rest.js
    └── k6-grpc.js
```

## How to run

### REST
```bash
cd rest-produtos-api
./mvnw spring-boot:run
# API at http://localhost:8080/api/v1/produtos
```

### gRPC
```bash
cd grpc-produtos-api
./mvnw spring-boot:run
# gRPC at localhost:9090 (use grpcurl or BloomRPC/Postman to test)
```

Example with `grpcurl`:
```bash
grpcurl -plaintext -d '{"pagina": 0, "tamanho": 20}' \
  localhost:9090 portfolio.produtos.v1.ProdutoService/ListarProdutos
```

## Equivalent endpoints / RPCs

| Action | REST | gRPC |
|---|---|---|
| Create | `POST /api/v1/produtos` | `ProdutoService/CriarProduto` |
| Get by id | `GET /api/v1/produtos/{id}` | `ProdutoService/BuscarProduto` |
| List (paginated) | `GET /api/v1/produtos?pagina=&tamanho=` | `ProdutoService/ListarProdutos` |
| Delete | `DELETE /api/v1/produtos/{id}` | `ProdutoService/DeletarProduto` |

---

## Benchmark

### Methodology

- **Tool:** [k6](https://k6.io) with the [xk6-grpc](https://github.com/grafana/xk6-grpc) extension for both protocols (same tool on both sides, to avoid measurement bias)
- **Environment:** _(fill in: machine specs — CPU, RAM, OS)_, services run one at a time (not simultaneously)
- **Warm-up:** 15s of warm-up (JIT) discarded before collection
- **Collection duration:** 60s per scenario
- **Payload:** `Produto` object with 5 fields (name, description, price, stock, timestamp)
- **Load scenarios:** 10, 50, 100, and 500 concurrent virtual users (VUs)
- **Measured operations:** `GET/List` (paginated listing of 20 items) and `POST/Create`
- **Run date:** _(fill in)_

### Results

> Run the commands in the [Reproducing the benchmark](#reproducing-the-benchmark) section
> and paste the `k6` output (or the values extracted from it) into the tables below.

#### Latency — list operation (20 items), by concurrent load

| VUs | REST p50 | REST p95 | REST p99 | gRPC p50 | gRPC p95 | gRPC p99 |
|---|---|---|---|---|---|---|
| 10  | | | | | | |
| 50  | | | | | | |
| 100 | | | | | | |
| 500 | | | | | | |

#### Sustained throughput (requests/second)

| VUs | REST (req/s) | gRPC (req/s) | Difference |
|---|---|---|---|
| 10  | | | |
| 50  | | | |
| 100 | | | |
| 500 | | | |

#### Payload and resource usage

| Metric | REST/JSON | gRPC/Protobuf |
|---|---|---|
| Average response size (list, 20 items) | | |
| Average CPU under 100 VUs | | |
| Average heap memory under 100 VUs | | |

### Reading the results

_(fill in after running: where gRPC/REST stood out, where the difference was bigger/smaller
than expected, behavior under high concurrency, etc.)_

### Reproducing the benchmark

```bash
# REST
k6 run --env VUS=100 --env DURATION=60s benchmark/k6-rest.js

# gRPC (requires a k6 build with xk6-grpc)
k6 run --env VUS=100 --env DURATION=60s benchmark/k6-grpc.js
```

---

## Tech stack

- Java 17
- Spring Boot 3.3
- Spring Web (REST)
- grpc-spring-boot-starter / net.devh (gRPC)
- Protocol Buffers 3
- k6 + xk6-grpc (load testing)

## Possible next steps

- Add real persistence (Postgres) to measure the impact of database I/O
- Expose metrics via Micrometer + Prometheus/Grafana to watch the tests in real time
- Add gRPC streaming (server-streaming) as a third comparison scenario
