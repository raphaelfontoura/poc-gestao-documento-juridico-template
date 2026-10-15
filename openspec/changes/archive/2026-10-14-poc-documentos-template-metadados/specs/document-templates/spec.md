# Spec Delta

## Purpose

Permite escolher modelos de peças jurídicas e obter arquivos editáveis com campos identificáveis para a posterior captura de metadados.

## ADDED Requirements

### Requirement: Catálogo de tipos de documento
O sistema SHALL apresentar ao usuário pelo menos os tipos parecer e portaria, cada um com seu template e conjunto de campos esperados.

#### Scenario: Seleção de tipo
- **WHEN** o usuário abre a criação de documento
- **THEN** o sistema apresenta parecer e portaria e identifica os metadados exigidos por cada tipo

### Requirement: Templates editáveis
O sistema SHALL fornecer um template editável em DOCX para cada tipo, com uma área estruturada de metadados e uma área de redação própria da peça.

#### Scenario: Download do template
- **WHEN** o usuário escolhe um tipo e solicita o template para edição sem acesso ao Drive
- **THEN** o sistema entrega o DOCX correspondente àquele tipo

### Requirement: Campos específicos por tipo
O sistema SHALL definir obrigatoriedade e identificação de cada campo por tipo de documento, sem exigir de uma portaria campos exclusivos de parecer.

#### Scenario: Campos de portaria
- **WHEN** o usuário seleciona portaria
- **THEN** o sistema apresenta somente os campos definidos para portaria e não exige ementa ou procuradores assinantes apenas por estarem presentes no template de parecer

