package com.portfolio.grpcprodutos.service;

import com.portfolio.grpcprodutos.model.Produto;
import com.portfolio.grpcprodutos.repository.ProdutoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mesma lógica de negócio do rest-produtos-api. Só muda quem chama:
 * ali é um @RestController, aqui é um serviço gRPC.
 */
@Service
public class ProdutoService {

    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void seed() {
        for (int i = 1; i <= 20; i++) {
            Produto produto = new Produto(
                    UUID.randomUUID().toString(),
                    "Produto " + i,
                    "Descrição de exemplo do produto " + i,
                    BigDecimal.valueOf(19.90 + i),
                    100 - i,
                    Instant.now()
            );
            repository.salvar(produto);
        }
    }

    public Produto criar(String nome, String descricao, BigDecimal preco, int quantidadeEstoque) {
        Produto produto = new Produto(
                UUID.randomUUID().toString(),
                nome,
                descricao,
                preco,
                quantidadeEstoque,
                Instant.now()
        );
        repository.salvar(produto);
        return produto;
    }

    public Produto buscarPorId(String id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    public List<Produto> listarTodos() {
        return repository.listarTodos();
    }

    public boolean deletar(String id) {
        if (!repository.existe(id)) {
            return false;
        }
        repository.deletar(id);
        return true;
    }
}
