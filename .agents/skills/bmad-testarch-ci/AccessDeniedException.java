package br.pucrs.constrsw.oauth.domain.exception;

/** Token valido mas sem permissao para a operacao. Mapeada em HTTP 403. */
public class AccessDeniedException extends DomainException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
