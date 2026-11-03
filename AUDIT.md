# Classificação das telas (visant-frontend, passo 0)

| Rota | Classe | Variável de negócio | Persuasão presente |
|---|---|---|---|
| #/chamados | B. Trabalho | throughput: chamados triados por visita | nenhuma; os chips de status são o eixo de varredura e contagem zero não aparece |
| #/chamados/novo | B. Trabalho | taxa de conclusão da abertura | nenhuma; descrição e técnico ficam recolhidos atrás de botão |
| #/chamados/{id} | B. Trabalho, com a região de status em C. Confiança | qualidade da decisão e dado que não se perde | nenhuma; fechar pede confirmação e a tela só muda depois da resposta da API |
| #/clientes, #/categorias, #/tecnicos | B. Trabalho, com a exclusão em C. Confiança | integridade dos cadastros usados nos chamados | nenhuma; excluir pede confirmação e a falha por vínculo é explicada |