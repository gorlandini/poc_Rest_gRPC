# REST vs gRPC — Spring Boot Comparison

Two Spring Boot microservices that implement **the exact same business logic** (a simple Product registry), exposed through two different protocols:

| Project             | Protocol      | Port                              | Serialization  |
| ------------------- | ------------- | --------------------------------- | -------------- |
| `rest-produtos-api` | REST/HTTP 1.1 | `8080`                            | JSON (Jackson) |
| `grpc-produtos-api` | gRPC/HTTP 2   | `9090` (gRPC) / `8081` (actuator) | Protobuf       |

The goal is to isolate the **transport and serialization** overhead, not the business
logic itself: both projects share the same `Service`/`Repository` layer (H2 in-memory database), differing only in the "shell" — a `@RestController` on one side, a `@GrpcService` on the other.

## Structure

```
poc_REST_gRPC/
├── rest-produtos-api/      # Spring Boot + Spring Web
├── grpc-produtos-api/      # Spring Boot + grpc-spring-boot-starter (net.devh)
└── benchmark/              # k6 scripts to reproduce the load tests
    ├── k6-rest-50vus.js
    ├── k6-rest-500vus.js
    ├── k6-grpc-50vus.js
    ├── k6-grpc-500vus.js
    ├── k6-rest-payload-grande.js
    └── k6-grpc-payload-grande.js
```

## How to run

### REST

```
cd rest-produtos-api
./mvnw spring-boot:run
# API at http://localhost:8080/api/v1/produtos
```

### gRPC

```
cd grpc-produtos-api
./mvnw spring-boot:run
# gRPC at localhost:9090 (use grpcurl or BloomRPC/Postman to test)
```

Example with `grpcurl`:

```
grpcurl -plaintext -d '{"pagina": 0, "tamanho": 20}' \
  localhost:9090 portfolio.produtos.v1.ProdutoService/ListarProdutos
```

## Equivalent endpoints / RPCs

| Action           | REST                                    | gRPC                            |
| ---------------- | ---------------------------------------- | -------------------------------- |
| Create           | `POST /api/v1/produtos`                 | `ProdutoService/CriarProduto`   |
| Get by id        | `GET /api/v1/produtos/{id}`             | `ProdutoService/BuscarProduto`  |
| List (paginated) | `GET /api/v1/produtos?pagina=&tamanho=` | `ProdutoService/ListarProdutos` |
| Delete           | `DELETE /api/v1/produtos/{id}`          | `ProdutoService/DeletarProduto` |

---

## Benchmark

### Methodology

- **Tool:** [k6](https://k6.io) with the [xk6-grpc](https://github.com/grafana/xk6-grpc) extension for both protocols (same tool on both sides, to avoid measurement bias)
- **Environment:** Acer Predator PHN16-71 (Intel i7-13650HX, 16GB RAM, Windows 11) — local execution, services run one at a time (not simultaneously)
- **Database:** H2 in-memory, restarted (app restart) before every individual run to guarantee a clean dataset and a fair comparison between runs
- **Load profile:** ramp-up / peak / ramp-down (`stages`), identical for REST and gRPC in each scenario
- **Measured operations:** one paginated `List` + one `Create` per iteration
- **Two scenarios were tested:**
  1. **Generic / small payload** — short text fields, default page size (20 items)
  2. **Large payload** — ~3KB text in the `descricao` field, larger page size (100 items), database pre-seeded with 200 records before the load starts
- **Run date:** July 2026

### Results — Scenario 1: generic / small payload

**50 VUs** (5s ramp-up → 20s peak → 5s ramp-down)

| Metric | REST | gRPC | Difference |
| --- | --- | --- | --- |
| Throughput | 730.5 req/s | 636.5 req/s | REST ~15% higher |
| Completed iterations | 10,967 | 9,552 | REST ~15% more |
| Average latency | 6.41 ms | 15.2 ms | REST ~2.4x lower |
| p95 | 17.51 ms | 103.1 ms | REST ~5.9x lower |
| Failure rate | 0% | 0% | both stable |

**500 VUs** (10s ramp-up → 60s peak → 10s ramp-down)

| Metric | REST | gRPC | Difference |
| --- | --- | --- | --- |
| Throughput | 1,154.8 req/s | 1,168.6 req/s | gRPC ~1% higher (practically tied) |
| Completed iterations | 46,244 | 46,814 | practically tied |
| Average latency | 305.87 ms | 288.01 ms | gRPC ~6% lower |
| p95 | 1.10 s | 1.18 s | REST ~7% lower |
| Failure rate | 0% | 0% | both stable |

> In both protocols, the `p(95)<500ms` threshold was exceeded at 500 VUs (REST: 1.10s / gRPC: 1.18s), indicating the load saturates the local test machine's capacity at this concurrency level. Correctness `checks`, however, stayed at 100% success across all scenarios.

### Results — Scenario 2: large payload (100 VUs)

This scenario was intentionally designed to favor gRPC — a larger payload should, in theory, benefit more from Protobuf's compact binary encoding and lack of repeated field names compared to JSON.

| Metric | REST | gRPC | Difference |
| --- | --- | --- | --- |
| Throughput | 351.3 req/s | 294.2 req/s | REST ~19% higher |
| Completed iterations | 10,814 | 8,998 | REST ~20% more |
| Average latency | 178.7 ms | 216.7 ms | REST ~21% lower |
| p95 | 629.8 ms | 717.9 ms | REST ~14% lower |
| Failure rate | 0% | 0% | both stable |
| Total data received | 2.7 GB | 2.2 GB | — |
| Total data sent | 35 MB | 28 MB | gRPC ~20% lower |

### Reading the results

Across all three tested scenarios — including the one specifically designed to favor gRPC with larger payloads — **REST matched or outperformed gRPC in latency and throughput** in this local, loopback-network environment. The only measurable advantage gRPC showed was in **bytes sent over the wire** (~20% less with Protobuf in the large-payload scenario), which did not translate into lower latency here.

| Factor | Favors |
| --- | --- |
| Simplicity and universal compatibility (browsers, tooling, third parties) | REST |
| Internal microservice-to-microservice communication with a typed contract | gRPC |
| Bidirectional streaming / continuous events | gRPC |
| Latency under limited bandwidth or real network latency (not loopback) | gRPC (to be validated with a real-network test) |
| Fast debugging, human-readable payloads | REST |
| Data volume on the wire | gRPC |

These results are specific to this environment (same-machine execution, loopback network) and workload (simple CRUD). They don't imply gRPC is generally inferior to REST — they show that, **under the conditions tested here**, gRPC's theoretical performance edge didn't materialize. In practice, the choice between the two protocols should be guided more by architectural fit — typed contracts, streaming, client code generation, high-volume internal service communication — than by raw throughput benchmarks alone.

### Reproducing the benchmark

```bash
# 1. Start the application (REST or gRPC)
cd rest-produtos-api   # or grpc-produtos-api
./mvnw spring-boot:run

# 2. In another terminal, from the benchmark/ folder
k6 run --summary-export=resultado.json k6-rest-50vus.js
# other scripts: k6-rest-500vus.js, k6-grpc-50vus.js, k6-grpc-500vus.js,
# k6-rest-payload-grande.js, k6-grpc-payload-grande.js
```

> Restart the application (Ctrl+C and start it again) before each individual run to guarantee a clean H2 database.

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
- Run the benchmark across a real network (not loopback) to test whether gRPC's theoretical advantage appears under realistic latency/bandwidth conditions
- Expose metrics via Micrometer + Prometheus/Grafana to watch the tests in real time
- Add gRPC streaming (server-streaming) as a third comparison scenario
