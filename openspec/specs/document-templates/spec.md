# document-templates Specification

## Purpose

Permite escolher modelos de peças jurídicas e obter arquivos editáveis com campos identificáveis para a posterior captura de metadados.

## Requirements

### Requirement: Catálogo de tipos de documento
O sistema SHALL apresentar os tipos de manifestação disponíveis em um único combobox identificado como “Tipo de manifestação”, incluindo pelo menos parecer e portaria, cada um associado ao seu template e conjunto de campos esperados. As ações de criação e download SHALL usar exclusivamente o tipo selecionado.

#### Scenario: Seleção de tipo
- **WHEN** o usuário abre a criação de manifestação e o catálogo é carregado
- **THEN** o sistema apresenta parecer e portaria como opções do combobox e identifica os metadados exigidos pelo tipo selecionado

#### Scenario: Tipo não selecionado
- **WHEN** o usuário ainda não selecionou um tipo válido ou o catálogo não foi carregado
- **THEN** o sistema impede a criação no Google Docs e o download do modelo

#### Scenario: Mudança de seleção
- **WHEN** o usuário troca o tipo selecionado antes de acionar uma das opções
- **THEN** a ação escolhida utiliza o template correspondente à seleção mais recente

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
