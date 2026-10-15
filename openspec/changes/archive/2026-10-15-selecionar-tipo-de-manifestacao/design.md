# Design

## Context

A tela Angular de listagem renderiza um cartão por item de `GET /api/templates`. Cada cartão chama `POST /api/documents` com `templateId` e `useDrive`; a tela de detalhe oferece `GET /api/documents/{id}/template`. Também existe `GET /api/templates/{tipo}/download`, que baixa um modelo sem registro. O upload posterior exige um `id` de documento. Ver a motivação em [proposal.md](proposal.md).

## Goals / Non-Goals

**Goals:**

- Manter uma única fonte de seleção de tipo e impedir ações com seleção vazia ou desatualizada.
- Garantir que o caminho de DOCX tenha um registro antes do download, para que o upload posterior tenha destino.
- Preservar contratos da API e o tratamento atual de falha do Drive.

**Non-Goals:**

- Renomear entidades, rotas, identificadores de template ou estruturas persistidas no backend.
- Alterar o conteúdo dos modelos DOCX ou as regras de extração de metadados.

## Decisions

1. Usar um `<select>` nativo com rótulo “Tipo de manifestação” e opção inicial sem valor. As opções vêm de `GET /api/templates`; descrição e número de campos são derivados da opção selecionada. Os botões ficam indisponíveis enquanto o catálogo carrega, quando não há seleção válida ou durante uma operação. Um seletor nativo oferece teclado e tecnologias assistivas sem introduzir dependência; manter cartões selecionáveis exigiria mais estados e preservaria a repetição visual.
2. Manter `POST /api/documents` para ambos os botões. “Criar no Google Docs” envia `useDrive: true` e segue ao detalhe, onde a cópia pode ser aberta; uma falha do Drive continua registrada e apresentada. “Baixar modelo” envia `useDrive: false`, baixa `GET /api/documents/{id}/template` e segue ao detalhe desse `id`. Usar diretamente `/api/templates/{tipo}/download` não criaria o registro exigido pelo upload.
3. No download, verificar a resposta HTTP, usar o nome de arquivo fornecido pelo catálogo e disparar o download pelo navegador antes da navegação. Se a criação funcionar e o download falhar, exibir a falha com acesso ao detalhe já criado; o detalhe mantém o link de nova tentativa. Evitar nova criação na tentativa de recuperar o download impede registros duplicados.
4. Atualizar os textos visíveis da listagem/criação e as instruções do README para “manifestação”, mantendo Parecer e Portaria como nomes dos tipos. Concentrar a alteração na apresentação evita quebrar integrações que usam `templateId` ou as rotas `/api/documents`.

## Risks / Trade-offs

- [Download bloqueado ou interrompido pelo navegador] → Acionar o download dentro da ação do usuário e manter o link do modelo na tela do registro para nova tentativa.
- [Registro criado sem arquivo baixado] → Mostrar o erro e o caminho para o registro existente, sem repetir automaticamente o POST.
- [Catálogo vazio ou falha de carregamento] → Manter as ações desabilitadas e mostrar o erro já previsto na tela.

## Migration Plan

Não há migração de dados nem mudança de contrato. A implantação troca apenas o fluxo e os textos do frontend; registros existentes continuam acessíveis. Uma reversão do frontend restaura a tela anterior sem ajuste no backend.
