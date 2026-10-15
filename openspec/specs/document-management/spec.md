# document-management Specification

## Purpose

Orquestra a criação, edição externa, importação, finalização e consulta de documentos elaborados a partir dos templates da POC.

## Requirements

### Requirement: Criação de documento a partir de template
O sistema SHALL registrar um novo documento associado ao tipo e ao template escolhidos e oferecer edição no Google Docs quando a integração estiver disponível.

#### Scenario: Criação com Drive disponível
- **WHEN** o usuário cria um documento e a conta Google de teste tem acesso ao template
- **THEN** o sistema cria uma cópia editável, associa seu identificador ao registro e oferece um link para edição

#### Scenario: Criação sem Drive disponível
- **WHEN** o usuário cria um documento sem acesso funcional ao Drive
- **THEN** o sistema mantém o registro e oferece o DOCX do template para edição e posterior upload

### Requirement: Finalização por duas origens
O sistema SHALL aceitar a finalização por leitura do documento associado no Drive ou por upload de um DOCX, aplicando a mesma validação de metadados nas duas origens.

#### Scenario: Finalização pelo Drive
- **WHEN** o usuário pede para finalizar um documento associado ao Drive e a exportação funciona
- **THEN** o sistema lê a versão atual, apresenta os metadados extraídos e permite confirmar a finalização

#### Scenario: Falha de acesso ao Drive
- **WHEN** a leitura pelo Drive falha por autorização ou indisponibilidade
- **THEN** o sistema mantém o documento em edição, explica a falha e oferece upload de DOCX para continuar

#### Scenario: Finalização por upload
- **WHEN** o usuário envia um DOCX para um documento em edição
- **THEN** o sistema apresenta os metadados extraídos e permite confirmar a finalização após a validação

### Requirement: Confirmação e consulta
O sistema SHALL salvar a origem, o arquivo DOCX usado na finalização e os metadados validados, e SHALL permitir consultar esses dados no painel.

#### Scenario: Confirmação bem-sucedida
- **WHEN** o usuário confirma a finalização com metadados válidos
- **THEN** o documento passa ao estado finalizado e o painel exibe os valores persistidos e a origem usada

#### Scenario: Validação sem confirmação
- **WHEN** o usuário visualiza os metadados extraídos mas não confirma a finalização
- **THEN** o documento permanece em edição e os dados não são apresentados como finalizados
