package br.com.sistemas.chamados.exception;

public class ChamadoFechadoException extends RuntimeException {
    public ChamadoFechadoException(String mensagem) {
        super(mensagem);
    }
}