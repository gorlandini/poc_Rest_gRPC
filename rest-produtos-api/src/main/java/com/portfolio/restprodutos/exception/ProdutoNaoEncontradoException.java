package com.portfolio.restprodutos.exception;

public class ProdutoNaoEncontradoException extends RuntimeException {

    public ProdutoNaoEncontradoException(String id) {
        super("Produto não encontrado: " + id);
    }
}
