# metadata-extraction Specification

## Purpose

Extrai e valida metadados de arquivos DOCX produzidos a partir dos templates, preservando o vínculo entre cada valor e seu campo definido.

## Requirements

### Requirement: Extração estruturada
O sistema SHALL localizar os campos identificados no template do tipo selecionado e devolver seus valores a partir de um DOCX, inclusive quando esse DOCX tiver sido exportado do Google Docs.

#### Scenario: Campos preenchidos
- **WHEN** o DOCX mantém a área estruturada e todos os campos exigidos estão preenchidos
- **THEN** o sistema retorna cada valor sob seu campo correspondente

#### Scenario: Campos repetíveis
- **WHEN** um campo de interessados ou assinantes contém mais de uma pessoa conforme a convenção do template
- **THEN** o sistema retorna cada pessoa como um item separado

### Requirement: Validação antes da finalização
O sistema SHALL informar campos obrigatórios vazios, estrutura ausente ou valores inválidos e SHALL impedir a confirmação enquanto houver erros.

#### Scenario: Campo obrigatório vazio
- **WHEN** um campo exigido para o tipo está vazio
- **THEN** o sistema identifica o campo e impede a confirmação

#### Scenario: Estrutura alterada
- **WHEN** a área de metadados foi removida ou um identificador exigido não pode ser localizado
- **THEN** o sistema informa que o documento não pôde ser lido conforme o template e impede a confirmação

#### Scenario: Data inválida
- **WHEN** o valor do campo data não corresponde ao formato aceito no template
- **THEN** o sistema informa o erro de data e impede a confirmação

### Requirement: Arquivo compatível
O sistema SHALL aceitar DOCX na importação e SHALL rejeitar arquivos de outro formato com uma mensagem que informe o formato esperado.

#### Scenario: Upload de formato não aceito
- **WHEN** o usuário envia um PDF ou outro arquivo que não seja DOCX
- **THEN** o sistema recusa a extração e orienta a exportar o documento como DOCX
