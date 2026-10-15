# Tasks

## 1. Estrutura da POC

- [x] 1.1 Criar os projetos Spring Boot e Angular com comandos locais de execução; verificar que ambos iniciam e que o frontend alcança um endpoint de saúde do backend.
- [x] 1.2 Configurar SQLite e o esquema de templates, documentos, prévias e metadados; verificar que o banco é criado e preserva um registro após reiniciar o backend.

## 2. Templates e catálogo

- [x] 2.1 Criar DOCX fictícios de parecer e portaria com tabela `METADADOS_POC_V1`, campos próprios e instruções de redação sem dados preenchidos; verificar que ambos abrem no Google Docs e conservam tabela e chaves após exportação para DOCX.
- [x] 2.2 Carregar templates e definições de campos no banco e expor catálogo e download; verificar que cada tipo lista seus campos corretos e baixa o DOCX correspondente.
- [x] 2.3 Documentar a origem estrutural dos modelos públicos e a convenção de preenchimento (uma pessoa por linha e data `dd/MM/aaaa`); verificar que as instruções permitem preencher os dois templates sem consultar o código.

## 3. Extração de metadados

- [x] 3.1 Implementar leitura da tabela identificada em DOCX e retorno de campos escalares e listas; verificar com testes de parecer e portaria preenchidos, inclusive um DOCX exportado do Google Docs.
- [x] 3.2 Validar campos obrigatórios por tipo, data e estrutura do template; verificar com testes de campo vazio, data inválida, marcador removido, chave ausente e portaria sem ementa.
- [x] 3.3 Validar formato e tamanho do arquivo recebido; verificar que PDF, arquivo corrompido e DOCX acima do limite retornam erros úteis sem alterar o documento.

## 4. Gestão e finalização no backend

- [x] 4.1 Implementar criação, listagem, detalhe e download de template para documentos em edição; verificar pela API que o registro mantém tipo, template e estado após reinício.
- [x] 4.2 Implementar prévia por upload com arquivo temporário associado ao documento e confirmação da versão validada; verificar que somente uma prévia válida pode ser confirmada e que nova prévia invalida a anterior.
- [x] 4.3 Persistir DOCX finalizado, origem e metadados; verificar pela API que detalhe e download retornam exatamente os dados e bytes confirmados.
- [x] 4.4 Documentar os endpoints e exemplos de erros de validação; verificar as chamadas descritas com uma sequência completa de criação, upload e finalização.

## 5. Integração com Google Drive

- [x] 5.1 Configurar autorização da conta Google de teste por variáveis/credenciais externas ao repositório e cópia do template no Drive; verificar com conta configurada que a criação retorna ID e link editável, sem expor tokens ao navegador.
- [x] 5.2 Exportar a versão atual do Google Docs para DOCX e enviá-la ao mesmo fluxo de prévia/confirmação; verificar que os metadados de um documento editado no Google Docs coincidem com os de seu DOCX exportado e enviado por upload.
- [x] 5.3 Tratar ausência de credenciais, permissão negada e falha de exportação; verificar que o documento permanece em edição e aceita upload de DOCX no mesmo registro.
- [x] 5.4 Documentar a configuração da conta de teste e o fluxo alternativo sem Drive; verificar que a POC inicia e permite finalizar por upload sem credenciais Google.

## 6. Painel Angular

- [x] 6.1 Criar lista, seleção de tipo e página de documento com link para Google Docs ou download do template; verificar a navegação e os dois caminhos de criação no navegador.
- [x] 6.2 Criar upload, prévia de metadados, mensagens por campo e confirmação; verificar no navegador um DOCX válido, um inválido e a troca de prévia antes da confirmação.
- [x] 6.3 Exibir origem, estado e metadados persistidos no detalhe; verificar que recarregar a página mantém os dados de um documento finalizado.

## 7. Integração da POC

- [x] 7.1 Executar os fluxos completos de parecer e portaria por upload e pelo Drive quando houver conta configurada; verificar que os mesmos campos são extraídos e que as falhas do Drive mantêm o caminho manual disponível.
