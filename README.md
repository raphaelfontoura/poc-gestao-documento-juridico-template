# POC de documentos com metadados

Aplicação de demonstração para criar pareceres e portarias a partir de modelos DOCX, extrair seus metadados e guardar a versão final confirmada. A edição pode ser feita em um arquivo local ou, opcionalmente, em uma cópia no Google Docs.

O projeto tem um frontend Angular, uma API Spring Boot e um banco SQLite local. Os modelos contêm uma tabela identificada por `METADADOS_POC_V1`; o backend lê e valida seus campos antes de finalizar o documento.

## Requisitos

- Java 21 ou superior e Maven.
- Node.js 24 e npm.
- Uma conta Google de teste somente se quiser usar a integração com Drive.

## Executar localmente

Inicie o backend na pasta do projeto:

```bash
cd backend
mvn spring-boot:run
```

Em outro terminal, inicie o frontend:

```bash
cd frontend
npm ci
npm start
```

Acesse <http://localhost:4200>. A API responde em <http://localhost:8080/api/health>; o servidor de desenvolvimento do Angular encaminha as chamadas `/api` para a porta 8080. O banco é criado em `backend/data/poc.db` quando o backend é iniciado a partir da pasta `backend/`. Esse diretório é ignorado pelo Git.

> [!NOTE]
> Não é preciso configurar credenciais para experimentar o fluxo com upload de DOCX.

## Usar a aplicação

1. No combobox **Tipo de manifestação**, escolha **Parecer** ou **Portaria** e clique em **Baixar modelo**. A aplicação cria o registro da manifestação, baixa o DOCX do tipo escolhido e abre a tela desse registro.
2. Preencha a coluna **Valor** da tabela de metadados e salve o arquivo em formato `.docx`. Preserve o marcador `METADADOS_POC_V1` e os nomes das chaves. O link **Baixar modelo DOCX** na tela do registro permite repetir o download.
3. Envie o DOCX para gerar uma prévia. Corrija os campos indicados se houver erros.
4. Confirme a prévia válida para persistir os metadados e o arquivo. Depois, consulte o registro ou baixe o DOCX finalizado.

Os campos exigidos e as regras de preenchimento de cada modelo estão em [docs/templates.md](docs/templates.md). O sistema aceita DOCX de até 5 MB e permite substituir a prévia antes da confirmação. Após a finalização, o documento não aceita novas edições.

### Integração opcional com Google Drive

Crie o arquivo local de configuração a partir do modelo versionado:

```bash
cd backend
cp -n .env.example .env
```

O comando preserva um `.env` já existente. Preencha nele as variáveis `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REFRESH_TOKEN`, `GOOGLE_TEMPLATE_PARECER_ID` e `GOOGLE_TEMPLATE_PORTARIA_ID`. Os dois IDs de template devem apontar para **modelos em branco convertidos em Google Docs nativos** na conta de teste. Em seguida, carregue as variáveis no mesmo terminal em que iniciará o backend:

```bash
source .env
mvn spring-boot:run
```

O arquivo `.env` é ignorado pelo Git; mantenha os valores reais apenas nele ou em outro armazenamento seguro. O comando `source` precisa ser repetido em cada novo terminal. Para obter as credenciais OAuth, preparar os modelos no Drive e resolver erros de autorização, siga [docs/google-drive.md](docs/google-drive.md).

Com a integração configurada, escolha o **Tipo de manifestação** e clique em **Criar no Google Docs**. Edite a cópia pelo link exibido e use **Ler versão atual do Drive** para gerar a prévia. Se a cópia ou a leitura falhar, o registro permanece disponível para baixar o modelo e seguir pelo fluxo de upload de DOCX.

## Estrutura do projeto

| Pasta | Conteúdo |
| --- | --- |
| [`backend/`](backend/) | API Spring Boot, validação e extração de DOCX, persistência SQLite e integração com Drive. |
| [`frontend/`](frontend/) | Interface Angular para criação, edição, prévia e consulta dos documentos. |
| [`docs/`](docs/) | Detalhes da API, preenchimento dos modelos e configuração do Drive. |
| [`openspec/`](openspec/) | Especificações e histórico da mudança que originou esta POC. |

## API e testes

A URL base da API é `http://localhost:8080/api`. As rotas, exemplos com `curl` e a sequência de confirmação estão em [docs/api.md](docs/api.md).

Para executar os testes:

```bash
cd backend
mvn test
```

```bash
cd frontend
npm ci
npm test
```
