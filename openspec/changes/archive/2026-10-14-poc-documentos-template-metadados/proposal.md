# Proposal

## Why

Procuradores elaboram peças no Google Docs, mas o cadastro posterior de interessados, assunto, ementa, data e assinantes exige trabalho manual. Uma POC deve verificar se templates estruturados permitem recuperar esses dados com previsibilidade, tanto por integração com o Drive quanto por envio de DOCX.

## What Changes

- Criar um painel para escolher um tipo de documento, iniciar sua elaboração e consultar seus metadados.
- Disponibilizar templates fictícios de parecer e portaria, inspirados em modelos públicos, com campos de metadados definidos por tipo.
- Permitir a criação de uma cópia editável no Google Docs usando uma conta Google de teste e oferecer download do template em DOCX quando a integração não estiver disponível.
- Finalizar pelo acesso ao Drive ou por upload de DOCX, extrair os campos do documento e mostrar erros de validação antes de confirmar a finalização.
- Persistir templates, documentos, arquivos DOCX de finalização e metadados em SQLite para demonstração local.

## Capabilities

### New Capabilities

- `document-templates`: catálogo, campos esperados e disponibilização dos templates por tipo.
- `document-management`: criação, edição externa, importação, finalização e consulta de documentos.
- `metadata-extraction`: leitura e validação dos campos estruturados em DOCX.

### Modified Capabilities

Nenhuma.

## Impact

Projeto inicialmente sem código de aplicação. A mudança introduz frontend Angular/TypeScript, backend Java/Spring Boot, SQLite, integração com Google Drive e processamento de DOCX. A POC usa dados e conteúdo jurídico fictícios; os modelos públicos servem de referência estrutural e não de orientação jurídica.
