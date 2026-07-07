package com.portfolio.restprodutos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ProdutoDTOs {

    public record ProdutoRequest(
            @NotBlank String nome,
            String descricao,
            @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal preco,
            @NotNull @Min(0) Integer quantidadeEstoque
    ) {
    }

    public record ProdutoResponse(
            String id,
            String nome,
            String descricao,
            BigDecimal preco,
            Integer quantidadeEstoque,
            Instant criadoEm
    ) {
    }

    public record ProdutoPageResponse(
            List<ProdutoResponse> itens,
            int pagina,
            int tamanhoPagina,
            long totalItens
    ) {
    }
}
