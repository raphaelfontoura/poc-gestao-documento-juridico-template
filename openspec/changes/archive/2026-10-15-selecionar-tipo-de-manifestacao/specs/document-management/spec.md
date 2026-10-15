# Spec Delta

## MODIFIED Requirements

### Requirement: Criação de documento a partir de template
O sistema SHALL registrar uma nova manifestação associada ao tipo e ao template selecionados. A criação pelo Google Docs SHALL oferecer edição na cópia do Drive quando disponível; a ação “Baixar modelo” SHALL entregar o DOCX do tipo selecionado e manter um registro em edição para posterior upload.

#### Scenario: Criação com Drive disponível
- **WHEN** o usuário seleciona um tipo e aciona “Criar no Google Docs” e a conta Google de teste tem acesso ao template
- **THEN** o sistema cria uma cópia editável do template selecionado, associa seu identificador ao registro e oferece um link para edição

#### Scenario: Criação sem Drive disponível
- **WHEN** o usuário aciona “Criar no Google Docs” sem acesso funcional ao Drive
- **THEN** o sistema mantém o registro do tipo selecionado, informa a falha e oferece o DOCX do mesmo template para edição e posterior upload

#### Scenario: Download do modelo selecionado
- **WHEN** o usuário seleciona um tipo e aciona “Baixar modelo”
- **THEN** o sistema cria um registro em edição daquele tipo, inicia o download do DOCX correspondente e oferece a tela do registro para enviar o arquivo preenchido

#### Scenario: Falha ao baixar após criar o registro
- **WHEN** o registro é criado mas o download do modelo falha
- **THEN** o sistema informa a falha e mantém acessível o registro criado e uma opção para repetir o download

## ADDED Requirements

### Requirement: Terminologia da manifestação na interface
O sistema SHALL usar “manifestação” em vez de “peça” nos textos da interface que descrevem a criação, edição e consulta dos registros, preservando os nomes dos tipos Parecer e Portaria.

#### Scenario: Criação e consulta
- **WHEN** o usuário visualiza a tela de criação ou a lista de registros
- **THEN** os títulos, instruções e estados vazios referem-se a manifestações
