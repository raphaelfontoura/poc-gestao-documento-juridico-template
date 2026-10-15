# Conta Google de teste

A integração usa uma única conta Google de teste. O backend guarda as credenciais em variáveis de ambiente e nunca as envia ao navegador. O frontend recebe apenas o ID da cópia, o link de edição e mensagens de falha.

1. No Google Cloud, habilite a Drive API, configure a tela de consentimento OAuth e crie um cliente OAuth do tipo **Aplicativo Web**. Adicione `https://developers.google.com/oauthplayground` aos URIs de redirecionamento autorizados. No [OAuth 2.0 Playground](https://developers.google.com/oauthplayground/), selecione **Use your own OAuth credentials**, informe esse cliente, mantenha **Access type: Offline**, autorize o escopo indicado abaixo e troque o código por tokens. Guarde o refresh token retornado; consulte o [guia oficial de OAuth](https://developers.google.com/identity/protocols/oauth2/web-server).
2. Envie `backend/src/main/resources/templates/parecer.docx` e `backend/src/main/resources/templates/portaria.docx` ao Drive dessa conta e salve **cada um como Google Docs nativo**. Confira que o título mantém `[número]/[ano]`, que a tabela e o marcador estão presentes e que nenhum dado de exemplo foi preenchido. Em cada URL `https://docs.google.com/document/d/ID/edit`, copie o respectivo `ID`. O arquivo `backend/src/test/resources/google-docs/documento-test.docx` é um parecer **preenchido** para teste, não um template.
3. Conceda ao cliente OAuth acesso aos modelos. Para uma conta isolada de teste com documentos já existentes, o escopo `https://www.googleapis.com/auth/drive` permite copiar e exportar esses arquivos. Esse escopo é restrito; consulte a [orientação oficial sobre escopos](https://developers.google.com/workspace/drive/api/guides/api-specific-auth) antes de usar a integração fora da POC.
4. Configure as variáveis no ambiente do processo backend, fora do repositório:

```bash
export GOOGLE_CLIENT_ID='...'
export GOOGLE_CLIENT_SECRET='...'
export GOOGLE_REFRESH_TOKEN='...'
export GOOGLE_TEMPLATE_PARECER_ID='ID_DO_PARECER'
export GOOGLE_TEMPLATE_PORTARIA_ID='ID_DA_PORTARIA'
cd backend && mvn spring-boot:run
```

Não coloque esses valores em arquivos versionados nem no navegador. A cópia usa `files.copy`. A prévia exporta arquivos Google Docs nativos como DOCX; para uma cópia que ainda está em formato Office DOCX, baixa os bytes atuais do Drive. O Google limita a exportação de arquivos nativos a 10 MB; a POC aceita no máximo 5 MB no DOCX analisado. Uma cópia feita no Drive retorna um link editável. A confirmação guarda exatamente os bytes lidos naquela ocasião.

## Se a criação no Drive falhar

Inicie novamente o backend **no mesmo terminal** em que definiu as cinco variáveis. O `GOOGLE_CLIENT_ID`, o `GOOGLE_CLIENT_SECRET` e o `GOOGLE_REFRESH_TOKEN` devem pertencer ao mesmo cliente OAuth do tipo Aplicativo Web usado no Playground. Após reiniciar, tente criar outro documento no Drive e leia a mensagem exibida na página:

- **Refresh token inválido ou expirado:** obtenha outro token no Playground com esse mesmo cliente OAuth e atualize `GOOGLE_REFRESH_TOKEN` antes de reiniciar o backend. Para apps externos com tela de consentimento em modo *Testing*, o Google informa que o refresh token pode expirar em sete dias.
- **Client ID ou client secret rejeitado:** confira o par de credenciais do cliente Web no terminal do backend; não use o par do cliente Desktop criado anteriormente.
- **Cliente OAuth não autorizado:** confira o tipo e a configuração do cliente no Google Cloud.

Para verificar o trio de credenciais sem mostrar o access token, pare o backend e execute no **mesmo terminal** em que definiu as variáveis:

```bash
curl -sS https://oauth2.googleapis.com/token \
  --data-urlencode "client_id=$GOOGLE_CLIENT_ID" \
  --data-urlencode "client_secret=$GOOGLE_CLIENT_SECRET" \
  --data-urlencode "refresh_token=$GOOGLE_REFRESH_TOKEN" \
  --data-urlencode 'grant_type=refresh_token' \
  | jq '{ok: has("access_token"), error, error_description}'
```

Se aparecer `ok: true`, reinicie o backend nesse terminal. Se aparecer `unauthorized_client`, gere um refresh token novo no Playground depois de selecionar **Use your own OAuth credentials** com exatamente o cliente Web configurado nesse terminal.

Se uma nova cópia já aparecer com número, nomes ou assunto preenchidos, confira `GOOGLE_TEMPLATE_PARECER_ID` ou `GOOGLE_TEMPLATE_PORTARIA_ID`: o ID deve apontar para o modelo **em branco** criado a partir de `backend/src/main/resources/templates/`, não para `documento-test.docx` nem para um documento preenchido. Atualize o ID e reinicie o backend; cópias antigas permanecem como estavam.

Não publique o refresh token, o client secret ou a resposta completa do endpoint de token. Consulte a [documentação de erros OAuth](https://developers.google.com/identity/protocols/oauth2/web-server) e a [validade dos refresh tokens](https://developers.google.com/identity/protocols/oauth2).

## Sem Drive

Deixe as variáveis Google ausentes e inicie backend e frontend normalmente. Crie o documento sem solicitar o Drive, baixe o template, preencha e envie o DOCX. O fluxo de upload, prévia e confirmação funciona no mesmo registro mesmo após erro de credencial, permissão ou exportação do Drive. Veja [a sequência da API](api.md).
