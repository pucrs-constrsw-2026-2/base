package br.pucrs.constrsw.oauth.domain.model;

/**
 * Entidade de dominio: par de tokens emitidos pelo provedor de identidade
 * apos autenticacao com sucesso. Independe de Keycloak/HTTP/JSON.
 */
public final class AuthTokens {

    private final String tokenType;
    private final String accessToken;
    private final Long expiresIn;
    private final String refreshToken;
    private final Long refreshExpiresIn;

    public AuthTokens(String tokenType, String accessToken, Long expiresIn,
                      String refreshToken, Long refreshExpiresIn) {
        this.tokenType = tokenType;
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.refreshExpiresIn = refreshExpiresIn;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Long getRefreshExpiresIn() {
        return refreshExpiresIn;
    }
}
