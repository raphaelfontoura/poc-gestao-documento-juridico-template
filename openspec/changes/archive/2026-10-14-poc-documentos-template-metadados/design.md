# Design

## Context

O repositório contém apenas a configuração inicial do OpenSpec; não há aplicação, banco ou modelos existentes. A edição acontece no Google Docs, mas o fluxo precisa continuar quando o sistema não puder acessar o Drive. Ver `proposal.md` e os deltas em `specs/` para o comportamento esperado.

## Goals / Non-Goals

**Goals:**

- Usar um único contrato de extração para as duas origens de finalização.
- Tornar visíveis os erros causados por campos vazios, datas inválidas e alteração da estrutura do template.
- Permitir demonstração local com dados fictícios e uma conta Google de teste.

**Non-Goals:**

- Interpretar livremente o texto jurídico com IA ou inferir quem assinou de fato.
- Verificar assinatura digital, publicar atos, gerir aprovação jurídica ou integrar com processos institucionais.
- Suportar PDF, documentos arbitrários sem template, autenticação de procuradores ou edição simultânea dentro do painel.

## Decisions

### DOCX como entrada canônica

O backend exporta documentos do Drive para DOCX e aceita DOCX no upload. Ambos passam pelo mesmo extrator, baseado em Apache POI. Isso reduz diferenças de comportamento entre os caminhos e permite guardar o arquivo efetivamente analisado. Ler diretamente a árvore da API Google Docs exigiria um segundo extrator; PDF perderia a estrutura necessária para uma captura confiável. A exportação pelo Drive tem limite de tamanho, portanto a POC restringirá os documentos a arquivos pequenos e informará a falha quando excedido.

### Template com tabela de metadados identificada

Cada DOCX terá uma tabela de duas colunas no corpo do documento, precedida de um marcador de versão visível, por exemplo `METADADOS_POC_V1`. A primeira coluna usa chaves fixas e únicas; a segunda contém os valores editáveis. O extrator localizará o marcador e lerá a tabela seguinte, sem procurar palavras soltas pelo documento. A redação jurídica fica fora dessa tabela. A convenção dos campos repetíveis será uma pessoa por linha. A data aceita `dd/MM/aaaa` e é normalizada para armazenamento. Uma tabela identificada é mais simples de editar no Google Docs que intervalos nomeados, cuja extensão pode mudar durante a edição.

### Campos por tipo

Parecer exige interessados, assunto, ementa, data e procuradores assinantes. Portaria exige assunto, data e signatários; interessados e ementa podem aparecer como opcionais se fizerem sentido no exemplo. O catálogo define os campos e sua obrigatoriedade, permitindo adicionar novos tipos sem mudar a regra geral de extração. Os nomes em `procuradores assinantes` e `signatários` são declarações textuais do autor, sem comprovação de assinatura.

### Dois caminhos de criação e uma prévia antes da confirmação

No caminho integrado, o backend copia um template Google Docs mantido na conta de teste, guarda o ID da cópia e retorna o link de edição. No caminho manual, cria o registro e entrega o template DOCX para download. O usuário pode enviar um DOCX mesmo após uma falha no caminho integrado. A solicitação de finalização obtém o DOCX, extrai os dados e retorna uma prévia com erros por campo. A confirmação persiste arquivo, origem e metadados somente após prévia válida; o documento passa de `EM_EDICAO` para `FINALIZADO`. Uma nova tentativa de prévia substitui a anterior, para evitar confirmar valores de um arquivo antigo.

### Persistência e interface

O backend Spring Boot expõe endpoints para listar templates, criar/listar/consultar documentos, baixar template, obter prévia de finalização por Drive ou upload e confirmar a finalização. SQLite guarda tipos/templates, registros de documento, metadados e o DOCX finalizado como BLOB, adequado aos arquivos pequenos da POC. O frontend Angular apresenta lista, escolha do tipo, link ou download, upload, prévia, erros e detalhe do documento. Credenciais e tokens Google ficam fora do repositório e não são enviados ao frontend; a aplicação usa uma única conta de teste, sem identidade individual de procurador.

### Modelos públicos como referência

Os templates fictícios usarão como referência estrutural o [modelo de parecer no Manual da PGE/PA](https://www.pge.pa.gov.br/sites/default/files/manuais/MANUAL%20CONSULTORIA-2025.pdf) e os [modelos publicados pela PGM de Goiânia](https://www.goiania.go.gov.br/procuradoria/modelos-de-documentos/). A portaria poderá usar também o [modelo DOCX do portal do Espírito Santo](https://acessoainformacao.es.gov.br/). Os textos e nomes de exemplo serão originais e fictícios; os campos de captura serão adicionados especificamente para esta POC.

## Risks / Trade-offs

- [Usuário remove ou modifica a tabela] → Detectar marcador ou chave ausente e indicar o campo/estrutura a corrigir; não inferir valores do restante da peça.
- [Conversão Google Docs para DOCX altera formatação] → Testar o ciclo real de importação, edição e exportação dos dois templates; usar chaves textuais e tabela simples.
- [Conta Google sem permissão ou credencial vencida] → Exibir erro útil e oferecer o download/upload de DOCX no mesmo registro.
- [Alteração do Google Docs após a exportação] → Persistir exatamente o DOCX analisado e indicar ao usuário que a finalização captura uma versão naquele momento.
- [BLOBs aumentam o SQLite] → Limitar tamanho e quantidade de arquivos no escopo da POC; rever armazenamento externo para produção.
- [Dados pessoais em peças reais] → Demonstrar com dados fictícios; a adoção real exigirá desenho de acesso, retenção e proteção de documentos.

## Migration Plan

Não há sistema existente a migrar. A implantação inicial cria o esquema SQLite e carrega os dois templates. Para desfazer a POC local, encerrar os serviços e remover seus dados locais após exportar qualquer demonstração que se queira preservar.
