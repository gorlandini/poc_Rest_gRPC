package com.portfolio.restprodutos.service;

import com.portfolio.restprodutos.dto.ProdutoDTOs.ProdutoPageResponse;
import com.portfolio.restprodutos.dto.ProdutoDTOs.ProdutoRequest;
import com.portfolio.restprodutos.dto.ProdutoDTOs.ProdutoResponse;
import com.portfolio.restprodutos.exception.ProdutoNaoEncontradoException;
import com.portfolio.restprodutos.model.Produto;
import com.portfolio.restprodutos.repository.ProdutoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    public ProdutoResponse criar(ProdutoRequest request) {
        Produto produto = new Produto(
                UUID.randomUUID().toString(),
                request.nome(),
                request.descricao(),
                request.preco(),
                request.quantidadeEstoque(),
                Instant.now()
        );
        repository.salvar(produto);
        return toResponse(produto);
    }

    public ProdutoResponse buscarPorId(String id) {
        Produto produto = repository.buscarPorId(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
        return toResponse(produto);
    }

    public ProdutoPageResponse listar(int pagina, int tamanho) {
        List<Produto> todos = repository.listarTodos();
        int inicio = Math.min(pagina * tamanho, todos.size());
        int fim = Math.min(inicio + tamanho, todos.size());
        List<ProdutoResponse> itens = todos.subList(inicio, fim).stream()
                .map(this::toResponse)
                .toList();
        return new ProdutoPageResponse(itens, pagina, tamanho, todos.size());
    }

    public void deletar(String id) {
        if (!repository.existe(id)) {
            throw new ProdutoNaoEncontradoException(id);
        }
        repository.deletar(id);
    }

    private ProdutoResponse toResponse(Produto produto) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getPreco(),
                produto.getQuantidadeEstoque(),
                produto.getCriadoEm()
        );
    }
}
