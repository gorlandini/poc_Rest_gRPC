package com.portfolio.grpcprodutos.service;

public class ProdutoNaoEncontradoException extends RuntimeException {

    public ProdutoNaoEncontradoException(String id) {
        super("Produto não encontrado: " + id);
    }
}
