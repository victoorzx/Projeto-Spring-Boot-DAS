# Sistema de Chamados

API REST para gerenciamento de chamados de suporte, desenvolvida com Java 21 e Spring Boot na disciplina DAS (Design e Arquitetura de Sistemas), Engenharia de Software, Univille.

O projeto aplica arquitetura em camadas (Controller, Service, Repository, Entity, DTO e tratamento de exceções) e foi evoluído em etapas, cada uma com seu commit.

## Tecnologias

- Java 21 e Maven (Maven Wrapper incluso)
- Spring Boot 4.1.1: Spring Web MVC, Spring Data JPA, Bean Validation
- H2 em memória (console web habilitado)
- Testes com JUnit, Mockito, `@DataJpaTest` e `@WebMvcTest`

## Como executar

Na pasta do projeto (onde estão o `pom.xml` e o `mvnw`):

Windows PowerShell:

```powershell
.\mvnw spring-boot:run
```

macOS e Linux:

```bash
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. A rota `/` retorna 404, o que é esperado, pois não há controller mapeado para a raiz.

### Banco de dados (H2)

O banco é criado em memória a cada execução, então os dados são perdidos quando a aplicação reinicia.

- Console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:chamadosdb`
- Usuário: `sa`
- Senha: em branco

Exemplo de consulta no console: `SELECT * FROM CHAMADOS;`

## Estrutura de pacotes

Pacote raiz: `br.com.sistemas.chamados`

| Pacote | Responsabilidade |
| --- | --- |
| `controller` | Recebe HTTP, valida o corpo e devolve status e resposta |
| `service` | Regras de negócio e transações |
| `repository` | Acesso a dados com Spring Data JPA |
| `entity` | Modelo persistido (tabelas e enums) |
| `dto` | Contrato de entrada e saída da API |
| `exception` | Exceções de domínio e `GlobalExceptionHandler` |

## Modelo de dados

- `clientes`: nome, e-mail (único) e telefone
- `categorias`: nome (único) e descrição
- `tecnicos`: nome, e-mail (único) e perfil (`ADMIN` ou `TECNICO`)
- `chamados`: título, descrição, data de abertura, data de fechamento, prioridade, status, e relacionamento com cliente, categoria e técnico (opcional)
- `comentarios`: texto, data, autor e relacionamento com o chamado

Valores dos enums:

- Prioridade: `BAIXA`, `MEDIA`, `ALTA`, `CRITICA`
- StatusChamado: `ABERTO`, `EM_ATENDIMENTO`, `AGUARDANDO_CLIENTE`, `FECHADO`

## Rotas

| Método | Rota | Ação | Sucesso |
| --- | --- | --- | --- |
| POST | `/clientes` | Criar cliente | 201 |
| GET | `/clientes` | Listar clientes | 200 |
| GET | `/clientes/{id}` | Buscar cliente | 200 |
| PUT | `/clientes/{id}` | Atualizar cliente | 200 |
| DELETE | `/clientes/{id}` | Excluir cliente | 204 |
| GET | `/clientes/{id}/chamados` | Listar chamados do cliente (paginado) | 200 |
| POST | `/categorias` | Criar categoria | 201 |
| GET | `/categorias` | Listar categorias | 200 |
| GET | `/categorias/{id}` | Buscar categoria | 200 |
| PUT | `/categorias/{id}` | Atualizar categoria | 200 |
| DELETE | `/categorias/{id}` | Excluir categoria | 204 |
| POST | `/tecnicos` | Criar técnico | 201 |
| GET | `/tecnicos` | Listar técnicos | 200 |
| GET | `/tecnicos/{id}` | Buscar técnico | 200 |
| PUT | `/tecnicos/{id}` | Atualizar técnico | 200 |
| DELETE | `/tecnicos/{id}` | Excluir técnico | 204 |
| POST | `/chamados` | Abrir chamado | 201 |
| GET | `/chamados` | Listar chamados com filtros e paginação | 200 |
| GET | `/chamados/{id}` | Detalhe do chamado com comentários | 200 |
| PUT | `/chamados/{id}` | Atualizar dados do chamado | 200 |
| DELETE | `/chamados/{id}` | Excluir chamado | 204 |
| PATCH | `/chamados/{id}/atribuir` | Atribuir técnico | 200 |
| PATCH | `/chamados/{id}/status` | Alterar status | 200 |
| POST | `/chamados/{id}/comentarios` | Adicionar comentário | 201 |
| GET | `/chamados/{id}/comentarios` | Listar comentários | 200 |

## Corpos de requisição

Cliente:

```json
{ "nome": "Ana Souza", "email": "ana@exemplo.com", "telefone": "47999999999" }
```

Categoria:

```json
{ "nome": "Hardware", "descricao": "Problemas de equipamentos" }
```

Técnico:

```json
{ "nome": "Carlos Lima", "email": "carlos@empresa.com", "perfil": "TECNICO" }
```

Chamado (o campo `tecnicoId` é opcional):

```json
{
  "titulo": "Notebook não liga",
  "descricao": "Sem sinal de energia",
  "prioridade": "ALTA",
  "clienteId": 1,
  "categoriaId": 1,
  "tecnicoId": 1
}
```

Atribuir técnico (`PATCH /chamados/{id}/atribuir`):

```json
{ "tecnicoId": 1 }
```

Alterar status (`PATCH /chamados/{id}/status`):

```json
{ "status": "EM_ATENDIMENTO" }
```

Comentário:

```json
{ "texto": "Técnico a caminho", "autor": "Carlos Lima" }
```

## Regras de negócio

- O chamado sempre nasce com status `ABERTO` e a data de abertura é preenchida automaticamente.
- Ao fechar o chamado, a data de fechamento é preenchida automaticamente.
- Transições de status permitidas:

| De | Para |
| --- | --- |
| `ABERTO` | `EM_ATENDIMENTO`, `FECHADO` |
| `EM_ATENDIMENTO` | `AGUARDANDO_CLIENTE`, `FECHADO` |
| `AGUARDANDO_CLIENTE` | `EM_ATENDIMENTO`, `FECHADO` |
| `FECHADO` | nenhuma |

- Chamado fechado não pode ser atualizado, nem receber técnico, nem mudar de status (retorna 409).
- E-mail de cliente e de técnico e nome de categoria não podem se repetir (retorna 409).

## Consulta de chamados

Exemplos:

```
GET /chamados
GET /chamados?status=ABERTO
GET /chamados?prioridade=ALTA&clienteId=1
GET /chamados?status=EM_ATENDIMENTO&page=0&size=5&sort=dataAbertura,desc
GET /clientes/{id}/chamados
```

- Filtros opcionais: `status`, `prioridade` e `clienteId`, que podem ser combinados.
- Paginação: `page` (começa em 0), `size` (padrão 10) e `sort` (padrão `dataAbertura,desc`).
- Resposta: `conteudo`, `pagina`, `tamanho`, `totalElementos` e `totalPaginas`.

## Respostas de erro

Todos os erros seguem o mesmo formato:

```json
{
  "momento": "2026-10-22T19:13:03.860094",
  "status": 404,
  "erro": "Categoria 1 não encontrada",
  "detalhes": []
}
```

| Status | Quando acontece |
| --- | --- |
| 400 | Dados inválidos (Bean Validation); os campos com erro vêm em `detalhes` |
| 404 | Recurso não encontrado |
| 409 | Regra de negócio violada (duplicidade, transição inválida, chamado fechado) |

## Exemplos de teste

Com a aplicação rodando. No Windows PowerShell, os exemplos usam `Invoke-WebRequest`, que mostra o status HTTP e evita problemas de aspas.

Criar cliente (201):

```powershell
$body = @{ nome = "Ana Souza"; email = "ana@exemplo.com"; telefone = "47999999999" } | ConvertTo-Json
Invoke-WebRequest -Method Post -Uri http://localhost:8080/clientes -ContentType "application/json" -Body $body
```

Criar categoria (201):

```powershell
$body = @{ nome = "Hardware"; descricao = "Problemas de equipamentos" } | ConvertTo-Json
Invoke-WebRequest -Method Post -Uri http://localhost:8080/categorias -ContentType "application/json" -Body $body
```

Criar técnico (201):

```powershell
$body = @{ nome = "Carlos Lima"; email = "carlos@empresa.com"; perfil = "TECNICO" } | ConvertTo-Json
Invoke-WebRequest -Method Post -Uri http://localhost:8080/tecnicos -ContentType "application/json" -Body $body
```

Abrir chamado (201, status `ABERTO`):

```powershell
$body = @{ titulo = "Notebook não liga"; descricao = "Sem sinal de energia"; prioridade = "ALTA"; clienteId = 1; categoriaId = 1 } | ConvertTo-Json
Invoke-WebRequest -Method Post -Uri http://localhost:8080/chamados -ContentType "application/json" -Body $body
```

Atribuir técnico (200):

```powershell
$body = @{ tecnicoId = 1 } | ConvertTo-Json
Invoke-WebRequest -Method Patch -Uri http://localhost:8080/chamados/1/atribuir -ContentType "application/json" -Body $body
```

Alterar status (200; uma transição inválida retorna 409):

```powershell
$body = @{ status = "EM_ATENDIMENTO" } | ConvertTo-Json
Invoke-WebRequest -Method Patch -Uri http://localhost:8080/chamados/1/status -ContentType "application/json" -Body $body
```

Adicionar comentário (201):

```powershell
$body = @{ texto = "Técnico a caminho"; autor = "Carlos Lima" } | ConvertTo-Json
Invoke-WebRequest -Method Post -Uri http://localhost:8080/chamados/1/comentarios -ContentType "application/json" -Body $body
```

Detalhe do chamado com comentários (200):

```powershell
Invoke-RestMethod http://localhost:8080/chamados/1 | ConvertTo-Json -Depth 5
```

Listar com filtros (a URL entre aspas é obrigatória por causa do `&`):

```powershell
Invoke-RestMethod "http://localhost:8080/chamados?status=ABERTO&prioridade=ALTA&page=0&size=5"
```

Em macOS e Linux, o mesmo teste com `curl`:

```bash
curl -i -X POST http://localhost:8080/clientes \
  -H "Content-Type: application/json" \
  -d '{"nome":"Ana Souza","email":"ana@exemplo.com","telefone":"47999999999"}'
```

Testes de erro:

- Dados inválidos (400): enviar `POST /clientes` com `nome` vazio e e-mail inválido.
- E-mail duplicado (409): repetir o `POST /clientes` com o mesmo e-mail.
- Recurso inexistente (404): `GET /clientes/99999`.

## Testes automatizados

```powershell
.\mvnw test
```

Os testes ficam em `src/test/java`:

- `ClienteRepositoryTest` e `ChamadoRepositoryTest` (`@DataJpaTest`): persistência, unicidade de e-mail, filtros e paginação.
- `ClienteControllerTest` e `ChamadoControllerTest` (`@WebMvcTest`): status 201, 400, 404 e 409, regras de status e chamado fechado.

## Versionamento

- Branch de desenvolvimento: `feature/victor-developer`
- Os commits seguem o padrão `feat:`, `test:` e `chore:`, com um commit por etapa do desenvolvimento.