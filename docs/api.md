# API local da POC

Inicie o backend com `cd backend && mvn spring-boot:run`. A URL base é `http://localhost:8080/api`. O frontend usa as mesmas rotas pelo proxy `/api`.

| Método | Caminho | Resultado |
| --- | --- | --- |
| GET | `/health` | Estado do backend. |
| GET | `/templates` | Tipos de documento e campos por tipo. |
| GET | `/templates/{tipo}/download` | DOCX do tipo `parecer` ou `portaria`. |
| POST | `/documents` | Cria documento em edição; JSON `{"templateId":"parecer","useDrive":false}`. Com `useDrive:true`, tenta copiar o template no Drive. |
| GET | `/documents` | Lista documentos. |
| GET | `/documents/{id}` | Detalhe, origem e metadados após finalização. |
| GET | `/documents/{id}/template` | DOCX do template associado ao documento. |
| POST | `/documents/{id}/previews/upload` | Recebe formulário multipart com campo `file`; retorna prévia e erros por campo. |
| POST | `/documents/{id}/previews/drive` | Exporta a cópia atual do Google Docs como DOCX e aplica a mesma validação. |
| POST | `/documents/{id}/previews/{previewId}/confirm` | Confirma somente a prévia válida e atual. |
| GET | `/documents/{id}/file` | DOCX finalizado, com os mesmos bytes da prévia confirmada. |

## Sequência completa por upload

```bash
curl -s http://localhost:8080/api/templates
curl -s -H 'Content-Type: application/json' \
  -d '{"templateId":"parecer"}' http://localhost:8080/api/documents
```

Copie o `id` da resposta e baixe o modelo desse documento:

```bash
curl -L -o parecer.docx http://localhost:8080/api/documents/ID/template
```

Preencha a tabela do DOCX seguindo [as instruções](templates.md), substituindo todos os textos entre colchetes. Depois envie o arquivo:

```bash
curl -s -F 'file=@parecer.docx' http://localhost:8080/api/documents/ID/previews/upload
```

A resposta inclui `id` da prévia, `valid`, `fields` e `errors`. Se `valid` for `true`, confirme usando o ID retornado e consulte o resultado:

```bash
curl -s -X POST http://localhost:8080/api/documents/ID/previews/PREVIEW_ID/confirm
curl -s http://localhost:8080/api/documents/ID
curl -L -o finalizado.docx http://localhost:8080/api/documents/ID/file
```

É possível repetir o upload antes da confirmação. Cada novo upload substitui a prévia anterior, mesmo quando é inválido. Confirmar uma prévia antiga ou inválida retorna HTTP 409. Um documento finalizado não aceita outro upload.

Com `useDrive:true`, a resposta de criação inclui `driveFileId` e `editUrl` quando a cópia funciona. Se falhar, inclui `driveError` e mantém o registro para upload. Depois de editar no Google Docs, chame `POST /documents/ID/previews/drive` e confirme o `id` da prévia retornada da mesma forma. Falhas de exportação retornam HTTP 422 e mantêm o documento em edição.

## Erros de validação

Um upload com campos vazios ou data incorreta retorna HTTP 200 com `valid: false` e mensagens por campo, por exemplo:

```json
{
  "valid": false,
  "fields": { "assunto": "", "data": "31/02/2026", "signatarios": ["Ana Exemplo"] },
  "errors": {
    "assunto": "Campo obrigatório não preenchido.",
    "data": "Use uma data válida no formato dd/MM/aaaa."
  }
}
```

Um PDF, DOCX corrompido ou arquivo acima de 5 MB produz `valid: false` e uma mensagem em `errors.arquivo`. A falta do marcador ou da tabela produz `errors.estrutura`; uma chave ausente é apontada pelo nome dela. O documento continua em `EM_EDICAO` até a confirmação de uma prévia válida. URLs e IDs inexistentes retornam HTTP 404.
