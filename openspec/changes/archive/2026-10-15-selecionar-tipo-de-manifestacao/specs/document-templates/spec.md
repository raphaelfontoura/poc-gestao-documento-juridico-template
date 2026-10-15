# Spec Delta

## MODIFIED Requirements

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
