# Como preencher os templates

Baixe o modelo do tipo escolhido: `parecer.docx` ou `portaria.docx`. Eles ficam em `backend/src/main/resources/templates/` até o catálogo da aplicação estar disponível. É possível editar o DOCX em um editor compatível ou importá-lo no Google Docs e depois usar **Arquivo → Fazer download → Microsoft Word (.docx)**.

Cada modelo contém uma linha com o marcador `METADADOS_POC_V1` imediatamente antes de uma tabela de duas colunas. Preencha **somente a coluna Valor** dessa tabela. Mantenha o marcador, a tabela, os nomes das chaves e uma única linha para cada chave. A redação abaixo da tabela contém apenas títulos e instruções entre colchetes para substituir.

## Parecer

| Chave | O que preencher |
| --- | --- |
| `interessados` | Nome de cada interessado, uma pessoa por linha. Obrigatório. |
| `assunto` | Descrição curta do tema. Obrigatório. |
| `ementa` | Síntese do parecer. Obrigatório. |
| `data` | Data no formato `dd/MM/aaaa`, como `14/10/2026`. Obrigatório. |
| `procuradores_assinantes` | Nome de cada procurador declarado como assinante, uma pessoa por linha. Obrigatório. |

## Portaria

| Chave | O que preencher |
| --- | --- |
| `assunto` | Descrição curta do objeto da portaria. Obrigatório. |
| `data` | Data no formato `dd/MM/aaaa`. Obrigatório. |
| `signatarios` | Nome de cada signatário declarado, uma pessoa por linha. Obrigatório. |

Substitua inteiramente o texto entre colchetes. Não escreva vírgulas para separar pessoas: use quebras de linha dentro da célula. Os nomes representam declarações no texto, sem verificação de assinatura. Se a tabela ou alguma chave for removida, o sistema não conseguirá validar o arquivo. Use apenas dados fictícios nesta POC.

## Origem estrutural

O parecer usa a organização geral de identificação, relatório, análise e conclusão do [Manual da Consultoria Jurídica da PGE/PA, Anexo Único](https://www.pge.pa.gov.br/sites/default/files/manuais/MANUAL%20CONSULTORIA-2025.pdf). A portaria usa a forma de ato com preâmbulo, comando **RESOLVE** e artigos, inspirada nos [modelos publicados pela PGM de Goiânia](https://www.goiania.go.gov.br/procuradoria/modelos-de-documentos/). Textos, nomes, situações e tabela de metadados foram criados para esta demonstração; nenhum modelo é reprodução de orientação jurídica.
