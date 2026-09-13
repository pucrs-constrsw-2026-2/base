---
title: 'Bootstrap Java da API OAuth'
type: 'feature'
created: '2026-09-13'
status: 'done'
review_loop_iteration: 0
baseline_commit: '32147c632800fefbd8469708545619f3dd5ea6b2'
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O submódulo `backend/oauth` contém apenas um README e não possui uma aplicação executável, Dockerfile ou documentação Swagger. O `docker-compose.yml` também verifica a saúde do serviço com um comando Node.js, incompatível com a stack Java escolhida.

**Approach:** Criar um projeto Maven com Java 21 e Spring Boot 3.5.16, usando Spring MVC, Bean Validation, Actuator e springdoc-openapi. Entregar o menor fluxo executável: aplicação, configuração externa, endpoint `/health`, Swagger e imagem Docker, além de tornar o healthcheck do Compose compatível com Java.

## Boundaries & Constraints

**Always:** Manter o código da aplicação no submódulo `backend/oauth` e a branch atual `grupo02`; usar arquitetura em camadas `controller -> service -> client` quando as rotas funcionais forem adicionadas; usar Maven; ler porta e configuração do Keycloak das variáveis já fornecidas pelo Compose; expor Swagger em `/swagger-ui.html` e OpenAPI em `/v3/api-docs`; produzir uma imagem Docker que não dependa de artefatos compilados na máquina host.

**Ask First:** Alterar os contratos das rotas do enunciado, trocar a versão principal do Java ou Spring Boot, adicionar banco de dados, publicar commits ou enviar alterações ao GitHub.

**Never:** Implementar ainda as rotas de login, usuários ou roles; adicionar JPA, banco, Lombok, Spring Security ou Keycloak SDK; criar interfaces com uma única implementação; inserir credenciais no código ou no repositório.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Aplicação saudável | `GET /health` com a aplicação iniciada | HTTP 200 com JSON indicando estado saudável | Falha de inicialização deve encerrar o processo e aparecer nos logs |
| Swagger disponível | Acesso a `/swagger-ui.html` | Interface Swagger carrega e referencia `/v3/api-docs` | Configuração inválida deve falhar no teste de contexto |
| Porta via Compose | `OAUTH_INTERNAL_API_PORT=3001` | Aplicação escuta na porta 3001 | Usa 3001 como valor padrão quando a variável não existir |

</frozen-after-approval>

## Code Map

- `backend/oauth/pom.xml` -- dependências, Java 21 e build Maven.
- `backend/oauth/src/main/java/br/pucrs/constrsw/oauth/OAuthApplication.java` -- entrada da aplicação.
- `backend/oauth/src/main/java/br/pucrs/constrsw/oauth/controller/HealthController.java` -- endpoint mínimo de saúde.
- `backend/oauth/src/main/java/br/pucrs/constrsw/oauth/config/OpenApiConfig.java` -- metadados básicos da API no Swagger.
- `backend/oauth/src/main/resources/application.yml` -- porta, nome e caminhos do Swagger.
- `backend/oauth/src/test/java/br/pucrs/constrsw/oauth/OAuthApplicationTests.java` -- teste de inicialização e saúde.
- `backend/oauth/Dockerfile` -- build Maven e runtime Java em múltiplos estágios.
- `backend/oauth/.dockerignore` -- reduz o contexto de build.
- `backend/oauth/README.md` -- execução local, Docker e URLs úteis.
- `docker-compose.yml` -- healthcheck HTTP compatível com a imagem Java.

## Tasks & Acceptance

**Execution:**
- [x] Criar o projeto Maven Spring Boot com Web, Validation, Actuator, testes e springdoc compatível.
- [x] Criar a aplicação e o endpoint `/health` documentado pelo OpenAPI.
- [x] Configurar porta 3001, Swagger e variáveis do Keycloak sem armazenar segredos.
- [x] Criar Dockerfile multi-stage e `.dockerignore`.
- [x] Atualizar o healthcheck do serviço `oauth` no Compose para usar o endpoint Java.
- [x] Documentar os comandos e URLs no README.
- [x] Executar testes Maven e validar a configuração do Docker Compose.

**Acceptance Criteria:**
- Given Java 21 e Maven disponíveis, when `mvn test` é executado em `backend/oauth`, then o build e os testes terminam com sucesso.
- Given o serviço iniciado, when `/health`, `/v3/api-docs` e `/swagger-ui.html` são acessados, then retornam respostas válidas.
- Given o Compose com suas variáveis atuais, when a imagem OAuth é construída, then todo o build ocorre no Dockerfile e o healthcheck não depende de Node.js.

## Verification

**Commands:**
- `mvn test` -- build e testes aprovados.
- `docker compose config` -- configuração válida.
- `docker compose build oauth` -- imagem Java criada pelo Dockerfile.

## Suggested Review Order

**Application entry and API boundary**

- Bootstrap the Spring application and keep the runtime boundary small.
  [`OAuthApplication.java:6`](../../backend/oauth/src/main/java/br/pucrs/constrsw/oauth/OAuthApplication.java#L6)

- Expose the first health contract and make it visible in OpenAPI.
  [`HealthController.java:14`](../../backend/oauth/src/main/java/br/pucrs/constrsw/oauth/controller/HealthController.java#L14)

**Runtime packaging and deployment**

- Build and run the service with Java 21 as a non-root container user.
  [`Dockerfile:1`](../../backend/oauth/Dockerfile#L1)

- Require an exact HTTP 200 health response from the Compose service.
  [`docker-compose.yml:75`](../../docker-compose.yml#L75)

**Supporting configuration and verification**

- Define the Maven stack and Swagger dependency versions.
  [`pom.xml:16`](../../backend/oauth/pom.xml#L16)

- Document local, Compose, Swagger, and test entry points for the team.
  [`README.md:23`](../../backend/oauth/README.md#L23)

- Verify health and OpenAPI availability through the Spring test context.
  [`OAuthApplicationTests.java:17`](../../backend/oauth/src/test/java/br/pucrs/constrsw/oauth/OAuthApplicationTests.java#L17)
