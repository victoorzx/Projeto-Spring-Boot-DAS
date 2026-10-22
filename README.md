## Consulta de chamados

GET /chamados
GET /chamados?status=ABERTO
GET /chamados?prioridade=ALTA&clienteId=1
GET /chamados?status=EM_ATENDIMENTO&page=0&size=5&sort=dataAbertura,desc
GET /clientes/{id}/chamados

Filtros opcionais: status, prioridade e clienteId. Paginação: page (começa em 0), size (padrão 10) e sort.
Resposta: conteudo, pagina, tamanho, totalElementos e totalPaginas.