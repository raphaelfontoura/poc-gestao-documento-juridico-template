# Proposal

## Why

A criação atual repete as ações em cartões separados por modelo e chama os registros de “peças”. Um único seletor de tipo torna a escolha mais direta e permite usar “manifestação” de forma consistente na interface.

## What Changes

- Substituir os cartões de modelos por um combobox identificado como “Tipo de manifestação”, preenchido com os tipos retornados pelo catálogo (atualmente Parecer e Portaria).
- Oferecer, após a seleção, as ações “Criar no Google Docs” e “Baixar modelo”, ambas aplicadas somente ao tipo selecionado.
- No fluxo de download, criar uma manifestação em edição, baixar seu DOCX e abrir a tela do registro para posterior upload e finalização.
- Trocar “peça(s)” por “manifestação(ões)” nos textos voltados ao usuário e atualizar as instruções de uso da interface.
- Impedir as ações sem um tipo válido selecionado e informar erros de criação ou download sem perder a seleção.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `document-templates`: o catálogo passa a ser apresentado em um combobox único, e a ação de download usa o tipo selecionado.
- `document-management`: a criação a partir do tipo selecionado oferece caminhos explícitos para Google Docs e download do DOCX, preservando o registro para upload posterior.

## Impact

- Frontend Angular: tela de listagem/criação, estilos, textos e testes do fluxo.
- Documentação de uso do frontend e especificações de catálogo e criação de documentos.
- API existente de catálogo, criação e download de modelo; não se prevê alteração de contrato nem de dependências.
