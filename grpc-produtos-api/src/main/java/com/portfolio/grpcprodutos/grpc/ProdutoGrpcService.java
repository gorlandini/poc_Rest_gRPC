package com.portfolio.grpcprodutos.grpc;

import com.portfolio.grpcprodutos.grpc.proto.*;
import com.portfolio.grpcprodutos.model.Produto;
import com.portfolio.grpcprodutos.service.ProdutoNaoEncontradoException;
import com.portfolio.grpcprodutos.service.ProdutoService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.List;

/**
 * Equivalente ao ProdutoController do projeto REST: apenas traduz
 * chamadas gRPC para a mesma camada de serviço (ProdutoService).
 */
@GrpcService
public class ProdutoGrpcService extends ProdutoServiceGrpc.ProdutoServiceImplBase {

    private final ProdutoService service;

    public ProdutoGrpcService(ProdutoService service) {
        this.service = service;
    }

    @Override
    public void criarProduto(CriarProdutoRequest request, StreamObserver<ProdutoResponse> responseObserver) {
        Produto produto = service.criar(
                request.getNome(),
                request.getDescricao(),
                BigDecimal.valueOf(request.getPreco()),
                request.getQuantidadeEstoque()
        );
        responseObserver.onNext(toResponse(produto));
        responseObserver.onCompleted();
    }

    @Override
    public void buscarProduto(BuscarProdutoRequest request, StreamObserver<ProdutoResponse> responseObserver) {
        try {
            Produto produto = service.buscarPorId(request.getId());
            responseObserver.onNext(toResponse(produto));
            responseObserver.onCompleted();
        } catch (ProdutoNaoEncontradoException ex) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void listarProdutos(ListarProdutosRequest request, StreamObserver<ListarProdutosResponse> responseObserver) {
        List<Produto> todos = service.listarTodos();
        int pagina = request.getPagina();
        int tamanho = request.getTamanho() > 0 ? request.getTamanho() : 20;
        int inicio = Math.min(pagina * tamanho, todos.size());
        int fim = Math.min(inicio + tamanho, todos.size());

        ListarProdutosResponse.Builder builder = ListarProdutosResponse.newBuilder()
                .setPagina(pagina)
                .setTamanhoPagina(tamanho)
                .setTotalItens(todos.size());

        todos.subList(inicio, fim).forEach(p -> builder.addItens(toResponse(p)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void deletarProduto(DeletarProdutoRequest request, StreamObserver<DeletarProdutoResponse> responseObserver) {
        boolean sucesso = service.deletar(request.getId());
        responseObserver.onNext(DeletarProdutoResponse.newBuilder().setSucesso(sucesso).build());
        responseObserver.onCompleted();
    }

    private ProdutoResponse toResponse(Produto produto) {
        return ProdutoResponse.newBuilder()
                .setId(produto.getId())
                .setNome(produto.getNome())
                .setDescricao(produto.getDescricao() == null ? "" : produto.getDescricao())
                .setPreco(produto.getPreco().doubleValue())
                .setQuantidadeEstoque(produto.getQuantidadeEstoque())
                .setCriadoEmEpochMillis(produto.getCriadoEm().toEpochMilli())
                .build();
    }
}
