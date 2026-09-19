package br.pucrs.constrsw.oauth.domain.exception;

/** Usuario nao encontrado no provider. Mapeada em HTTP 404. */
public class UserNotFoundException extends DomainException {

    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
    }
}
