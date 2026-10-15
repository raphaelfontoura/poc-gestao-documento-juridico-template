# Tasks

## 1. Seleção e terminologia na interface

- [x] 1.1 Substituir os cartões da tela de criação por um `<select>` rotulado “Tipo de manifestação”, alimentado pelo catálogo, com opção inicial vazia e resumo dos campos do tipo escolhido; verificar no navegador que Parecer e Portaria aparecem e que mudar a seleção atualiza o resumo.
- [x] 1.2 Desabilitar as duas ações durante carregamento, operação ou seleção inválida, e ajustar os estilos do seletor para desktop e celular; verificar navegação por teclado, foco visível e estado desabilitado nos dois tamanhos.
- [x] 1.3 Trocar “peça(s)” por “manifestação(ões)” nos textos visíveis de criação e consulta e atualizar os testes da tela para seleção, estado vazio e terminologia; verificar que os testes do frontend passam.

## 2. Ações de criação e modelo

- [x] 2.1 Ligar “Criar no Google Docs” ao tipo selecionado usando a criação existente com `useDrive: true` e preservar a mensagem de falha do Drive; verificar em teste que o `templateId` enviado corresponde à seleção e que o registro permanece acessível se o Drive falhar.
- [x] 2.2 Ligar “Baixar modelo” à criação com `useDrive: false`, baixar o DOCX do registro criado e abrir seu detalhe; verificar em teste que o arquivo e o registro usam o mesmo tipo selecionado e que o detalhe permite upload posterior.
- [x] 2.3 Tratar falha de criação e falha de download sem perder a seleção nem criar um segundo registro na recuperação; verificar em teste que a falha é exibida, o registro já criado pode ser aberto e o download pode ser repetido no detalhe.
- [x] 2.4 Atualizar o README com o novo fluxo de seleção, Google Docs e download do modelo; verificar que as instruções correspondem aos rótulos e ações da interface.

## 3. Verificação integrada

- [x] 3.1 Executar os testes e o build do frontend e percorrer manualmente os dois caminhos com Parecer e Portaria, incluindo a visualização em tela estreita; verificar que o tipo selecionado chega ao registro correto, o download usa o modelo correspondente e o fluxo de upload continua disponível.
