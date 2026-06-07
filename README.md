# ggo-app-pax

## APIs utilizadas (ApiClient.java)

Base URL configurada via `Parametros.BASE_URL`. Todas as requisições são `POST` com corpo em JSON.

| Método | Endpoint | Descrição |
|---|---|---|
| `gerarToken` | `/oauth/token` | Gera token de acesso com `app-key` e `authorization` |
| `recuperar` | `/app/recupera` | Inicia recuperação de senha |
| `recuperar_verifica` | `/app/recupera/verifica` | Verifica código de recuperação |
| `recuperarEmail` | `/app/recupera/email` | Envia código de recuperação por e-mail |
| `recuperarFone` | `/app/recupera/fone` | Envia código de recuperação por SMS |
| `alterarSenha` | `/app/recupera/setpass` | Altera a senha após recuperação |
| `fazerLoginApi` | `/app/login` | Autentica o usuário (CPF + senha) |
| `buscarDadosCliente` | `/app/cliente/dados` | Busca dados do cliente (TB_CLI) |
| `buscarDependentes` | `/app/cliente/dependentes` | Busca dependentes (TB_DEPENDENTES) |
| `buscarParcelas` | `/app/cliente/parcelas` | Busca parcelas (TB_CX) |
| `buscarConveniados` | `/app/conveniados` | Busca convênios (TB_CONVENIADOS) |
| `buscarDadosCompletos` | `/app/cliente/dados_completos` | Busca dados do cliente com dependentes aninhados | 
