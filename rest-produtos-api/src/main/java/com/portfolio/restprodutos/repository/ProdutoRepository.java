package com.portfolio.restprodutos.repository;

import com.portfolio.restprodutos.model.Produto;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repositório em memória, propositalmente simples (sem banco de dados).
 * O objetivo do projeto é comparar o overhead de transporte (REST/JSON vs gRPC/Protobuf),
 * então a camada de persistência fica fora da equação.
 */
@Repository
public class ProdutoRepository {

    private final Map<String, Produto> dados = new ConcurrentHashMap<>();

    public Produto salvar(Produto produto) {
        dados.put(produto.getId(), produto);
        return produto;
    }

    public Optional<Produto> buscarPorId(String id) {
        return Optional.ofNullable(dados.get(id));
    }

    public List<Produto> listarTodos() {
        return dados.values().stream()
                .sorted((a, b) -> a.getCriadoEm().compareTo(b.getCriadoEm()))
                .collect(Collectors.toList());
    }

    public long contar() {
        return dados.size();
    }

    public boolean existe(String id) {
        return dados.containsKey(id);
    }

    public void deletar(String id) {
        dados.remove(id);
    }
}
