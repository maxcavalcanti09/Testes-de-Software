# Task Manager - API REST com Spring Boot, H2 e Testes Automatizados

Microsservico REST para gerenciamento de tarefas, desenvolvido com **Spring Boot 3.5**, **Java 21**, **Spring Data JPA**, **H2 em memoria**, **JUnit 5**, **Mockito** e **MockMvc**.

## Como executar

```bash
mvn spring-boot:run      # sobe a API em http://localhost:8080
mvn test                 # executa toda a suite de testes
```

Apos subir, a API fica disponivel em `http://localhost:8080` e o console do H2 em `http://localhost:8080/h2-console`
(URL JDBC `jdbc:h2:mem:testdb`, usuario `sa`, senha vazia).

## Endpoints

| Metodo | Endpoint            | Descricao                  | Sucesso | Erro                  |
|--------|---------------------|----------------------------|---------|-----------------------|
| POST   | `/api/tarefas`      | Cria uma nova tarefa       | 201     | 400                   |
| GET    | `/api/tarefas`      | Lista todas as tarefas     | 200     | -                     |
| GET    | `/api/tarefas/{id}` | Busca tarefa por ID        | 200     | 404                   |
| PUT    | `/api/tarefas/{id}` | Atualiza dados/status      | 200     | 400 / 404             |
| DELETE | `/api/tarefas/{id}` | Remove a tarefa            | 204     | 400 / 404             |

Exemplo de payload de criacao:

```json
{
  "titulo": "Implementar endpoint de tarefas",
  "descricao": "Metodo POST em /api/tarefas",
  "status": "PENDENTE"
}
```

## Arquitetura em camadas

```
src/main/java/com/empresa/taskmanager/
├── TaskManagerApplication.java
├── controller/     TarefaController.java            (@RestController, rotas e status HTTP)
├── service/        TarefaService.java               (@Service, regras de negocio)
├── repository/     TarefaRepository.java            (@Repository, JpaRepository e queries derivadas)
├── entity/         Tarefa.java, StatusTarefa.java   (@Entity, mapeamento JPA)
├── dto/            TarefaRequestDTO, TarefaAtualizacaoDTO, TarefaResponseDTO, ErroResponseDTO
└── exception/      RegraDeNegocioException, RecursoNaoEncontradoException, GlobalExceptionHandler
```

A entidade `Tarefa` nunca trafega pela rede: o Controller recebe DTOs, o Service converte para a
entidade e devolve `TarefaResponseDTO`.

## Regras de negocio implementadas

- `titulo` obrigatorio, entre 5 e 100 caracteres (validado por Bean Validation e tambem no Service).
- `descricao` opcional, ate 255 caracteres.
- `status` assume `PENDENTE` quando omitido; `dataCriacao` preenchida na criacao.
- `dataConclusao` preenchida automaticamente ao concluir e zerada ao voltar de `CONCLUIDA`.
- Nao e permitido duplicar titulo entre tarefas ativas (`status != CONCLUIDA`).
- Transicao direta de `PENDENTE` para `CONCLUIDA` e bloqueada; e preciso passar por `EM_ANDAMENTO`.
- Nao e possivel reabrir uma tarefa `CONCLUIDA` (estado final).
- Exclusao de tarefa `CONCLUIDA` e rejeitada com 400 Bad Request.
- ID inexistente retorna 404 Not Found em qualquer operacao.

Erros sao retornados de forma padronizada pelo `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-09-29T11:43:11.695299847",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Payload invalido",
  "erros": ["titulo: O titulo deve ter entre 5 e 100 caracteres"]
}
```

## Testes automatizados

37 testes automatizados, todos passando:

| Classe                            | Tipo                                    | Testes |
|-----------------------------------|-----------------------------------------|--------|
| `TarefaServiceTest`               | Unitario (JUnit 5 + Mockito)            | 19     |
| `TarefaControllerTest`            | Camada Web (`@WebMvcTest` + MockMvc)    | 10     |
| `TarefaIntegrationTest`           | Integracao (`@SpringBootTest` + H2)     | 8      |

`TarefaServiceTest` usa `@ExtendWith(MockitoExtension.class)`, `@Mock` no repositorio e
`@InjectMocks` no service, cobrindo caminho feliz (criacao, consulta, listagem, atualizacao,
exclusao) e cenarios de falha (titulo invalido, titulo duplicado, transicao proibida, tarefa
inexistente, exclusao de tarefa concluida), com `assertThrows`, `verify()`, `verify(never())`,
`times()` e `ArgumentCaptor`.

Os testes de integracao sobem o contexto completo com o H2 em memoria, usam o `data.sql` de carga
inicial e validam codigo HTTP e corpo JSON dos cinco endpoints.
