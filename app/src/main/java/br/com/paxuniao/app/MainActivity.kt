package br.com.paxuniao.app

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import br.com.paxuniao.app.ApiClient.ApiCallback
import br.com.paxuniao.app.Parametros.BASE_URL
import br.com.paxuniao.app.ui.theme.ClientesTheme
import okhttp3.RequestBody
import org.json.JSONObject


class MainActivity : ComponentActivity() {

    private lateinit var dados: Dados

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dados = Dados()
        dados.open(this)
        //dados.inserirDadosExemplo()
        val versao = dados.getDBVersion()
        Toast.makeText(this, "DB Version $versao", Toast.LENGTH_SHORT).show()

        enableEdgeToEdge()
        setContent {
            ClientesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Chamada da WebView passando o preenchimento do Scaffold
                    WebViewScreen(
                        //url = "file:///android_asset/login.html",  // Substitua pela sua URL
                        url = "file:///android_asset/contrato.html",  // Substitua pela sua URL

                        activity = this@MainActivity, // Passa a referência da activity
                        modifier = Modifier.padding(innerPadding),
                        dados = dados
                    )
                }
            }
        }


    }
}

class WebAppInterface(private val activity: ComponentActivity, private val webView: WebView, private val dados: Dados) {

    // Instância do cliente de API e variáveis para guardar o estado atual da recuperação
    private val apiClient = ApiClient()
    private var lastToken = ""
    private var currentEmails = org.json.JSONArray()
    private var currentFones = org.json.JSONArray()

    // Variáveis para guardar o estado da última solicitação de código
    private var lastCpfSolicitado = ""
    private var lastTipoSolicitado = ""
    private var lastIndexSolicitado = -1

    var resposta =""
    var code_valid = true

    @JavascriptInterface
    fun buscarDadosPorCPF(cpf: String) {
        apiClient.gerarToken( object : ApiClient.ApiCallback {
            override fun onSuccess(response: org.json.JSONObject) {
                lastToken = response.getString("access_token")

                apiClient.recuperar(lastToken, cpf, object : ApiClient.ApiCallback {
                    override fun onSuccess(recuperaResp: org.json.JSONObject) {
                        currentEmails = recuperaResp.optJSONArray("emails") ?: org.json.JSONArray()
                        currentFones = recuperaResp.optJSONArray("fones") ?: org.json.JSONArray()

                        // 1. Lendo corretamente os objetos JSON
                        val formattedEmails = org.json.JSONArray()
                        for (i in 0 until currentEmails.length()) {
                            // Pegamos o objeto e extraímos apenas o campo do e-mail
                            val emailObj = currentEmails.getJSONObject(i)
                            val emailOriginal = emailObj.getString("cli_email")
                            formattedEmails.put(emailOriginal)
                        }

                        val formattedFones = org.json.JSONArray()
                        for (i in 0 until currentFones.length()) {
                            // Pegamos o objeto e extraímos o ddd e fone
                            val foneObj = currentFones.getJSONObject(i)
                            val ddd = foneObj.getString("ddd")
                            val fone = foneObj.getString("fone")
                            formattedFones.put(ddd + fone)
                        }

                        val resultJson = org.json.JSONObject()
                        resultJson.put("emails", formattedEmails)
                        resultJson.put("telefones", formattedFones)

                        // 2. Converte para Base64 para envio seguro para o JS
                        val jsonString = resultJson.toString()
                        val base64Json = android.util.Base64.encodeToString(
                            jsonString.toByteArray(Charsets.UTF_8),
                            android.util.Base64.NO_WRAP
                        )

                        // 3. Envia para o JS
                        webView.post {
                            Log.i("dados", "receberDadosBase64('$base64Json')")
                            webView.evaluateJavascript("receberDadosBase64('$base64Json')", null)
                        }
                    }

                    override fun onError(error: String) {
                        showToast("Erro ao buscar CPF: $error")
                    }
                })
            }

            override fun onError(error: String) {
                showToast("Erro de autenticação: $error")
            }
        })
    }

    @JavascriptInterface
    fun setpass(code: String, pass: String, cpf: String) {
        Log.i("setpass", "Solicitando alteração de senha para CPF: $cpf")

        // 1. Gera um token novo para garantir que a requisição seja autorizada
        apiClient.gerarToken(object : ApiClient.ApiCallback {
            override fun onSuccess(response: org.json.JSONObject) {
                lastToken = response.getString("access_token")

                // 2. Chama a API para alterar a senha
                apiClient.alterarSenha(lastToken, cpf, code, pass, object : ApiClient.ApiCallback {
                    override fun onSuccess(setPassResp: org.json.JSONObject) {
                        val respStatus = setPassResp.optString("resp")

                        if (respStatus == "ok") {
                            showToast("Senha alterada com sucesso!")
                            webView.post {
                                webView.loadUrl("file:///android_asset/login.html")
                            }
                        } else {
                            val msgErro = setPassResp.optString("msg", "Erro ao alterar a senha.")
                            showToast(msgErro)

                            // DESCOMENTAR AQUI PARA REMOVER O LOADING NO HTML
                            webView.post { webView.evaluateJavascript("toggleLoading(false);", null) }
                        }
                    }

                    override fun onError(error: String) {
                        showToast("Erro de conexão ao alterar senha: $error")
                        // DESCOMENTAR AQUI TAMBÉM
                        webView.post { webView.evaluateJavascript("toggleLoading(false);", null) }
                    }
                })
            }

            override fun onError(error: String) {
                showToast("Erro de autenticação (Token): $error")
            }
        })
    }

    @JavascriptInterface
    fun enviarCodigoRecuperacao(cpfLimpo: String, tipo: String, index: Int) {
        Log.i("enviarCodigoRecuperacao", "$cpfLimpo ; $tipo ; $index")

        // 1. SALVANDO PARA UM POSSÍVEL REENVIO
        lastCpfSolicitado = cpfLimpo
        lastTipoSolicitado = tipo
        lastIndexSolicitado = index

        if (lastToken.isEmpty()) {
            showToast("Token inválido. Busque o CPF novamente.")
            return
        }

        if (tipo == "email") {
            val emailObj = currentEmails.optJSONObject(index)
            if (emailObj != null) {
                val email = emailObj.getString("cli_email")
                val cliCodigo = emailObj.getString("cli_codigo")

                apiClient.recuperarEmail(cpfLimpo, lastToken, email, cliCodigo, object : ApiClient.ApiCallback {
                    override fun onSuccess(response: org.json.JSONObject) {
                        val respStatus = response.optString("resp")
                        if (respStatus == "er") {
                            val msgErro = response.optString("msg", "Erro ao enviar e-mail.")
                            showToast(msgErro)
                        } else {
                            showToast("E-mail de recuperação enviado!")
                        }
                    }
                    override fun onError(error: String) {
                        showToast("Erro de conexão: $error")
                    }
                })
            }
        } else if (tipo == "sms") {
            val foneObj = currentFones.optJSONObject(index)
            if (foneObj != null) {
                val numeroCompleto = foneObj.getString("ddd") + foneObj.getString("fone")
                val seq = foneObj.getInt("seq")

                apiClient.recuperarFone(cpfLimpo, lastToken, numeroCompleto, seq.toString(), object : ApiClient.ApiCallback {
                    override fun onSuccess(response: org.json.JSONObject) {
                        val respStatus = response.optString("resp")
                        if (respStatus == "er") {
                            val msgErro = response.optString("msg", "Erro ao enviar SMS.")
                            showToast(msgErro)
                        } else {
                            showToast("SMS de recuperação enviado!")
                        }
                    }
                    override fun onError(error: String) {
                        showToast("Erro de conexão: $error")
                    }
                })
            }
        }
    }

    @JavascriptInterface
    fun reenviarCodigo() {
        if (lastCpfSolicitado.isNotEmpty() && lastTipoSolicitado.isNotEmpty() && lastIndexSolicitado != -1) {
            // Reaproveita a função de envio com os dados salvos em memória
            enviarCodigoRecuperacao(lastCpfSolicitado, lastTipoSolicitado, lastIndexSolicitado)
        } else {
            showToast("Erro ao reenviar: dados da solicitação anterior não encontrados.")
        }
    }

    @JavascriptInterface
    fun getDestinoVerificacao(): String {
        try {
            if (lastTipoSolicitado == "email" && lastIndexSolicitado != -1) {
                val emailObj = currentEmails.optJSONObject(lastIndexSolicitado)
                if (emailObj != null) {
                    val emailOriginal = emailObj.getString("cli_email")
                    // Utiliza sua função já existente para ofuscar
                    return emailOriginal
                }
            } else if (lastTipoSolicitado == "sms" && lastIndexSolicitado != -1) {
                val foneObj = currentFones.optJSONObject(lastIndexSolicitado)
                if (foneObj != null) {
                    val ddd = foneObj.getString("ddd")
                    val fone = foneObj.getString("fone")
                    // Utiliza sua função já existente para ofuscar
                    return ddd + fone
                }
            }
        } catch (e: Exception) {
            Log.e("getDestinoVerificacao", "Erro ao recuperar destino: ${e.message}")
        }

        // Retorno de fallback caso algo dê errado
        return "seu e-mail / telefone"
    }

    @JavascriptInterface
    fun verificarCodigo(cpf :String, code :String): Boolean {
        //showToast("Código: $cpf e $code");
        //val cpf="64293840397";
        try {
            apiClient.gerarToken( object : ApiClient.ApiCallback {
                override fun onSuccess(response: org.json.JSONObject) {
                    lastToken = response.getString("access_token")
                    apiClient.recuperar_verifica(
                        lastToken,
                        cpf,
                        code,
                        object : ApiClient.ApiCallback {
                            override fun onSuccess(recuperaResp: org.json.JSONObject) {
                                resposta = recuperaResp.get("resp").toString()
                                //showToast("Código: $resposta");
                                if (resposta == "ok") {
                                    code_valid = true
                                    ///"login_nova_senha.html?cpf=" + cpfUsuario + "&code=" + code);
                                    webView.post {
                                        webView.loadUrl("file:///android_asset/login_nova_senha.html?cpf=" + cpf + "&code=" + code)
                                    }
                                } else {
                                    showToast("Código inválido")
                                    webView.post {
                                        webView.evaluateJavascript("toggleLoading(false)", null)
                                    }
                                }
                            }

                            override fun onError(error: String) {
                                showToast("Erro ao buscar CPF: $error")
                                webView.post {
                                    webView.evaluateJavascript("toggleLoading(false)", null)
                                }

                            }
                        })
                }

                override fun onError(error: String) {
                    showToast("Erro de autenticação: $error")
                    webView.post {
                        webView.evaluateJavascript("toggleLoading(false)", null)
                    }
                }
            })
        } catch (e: Exception) {
            showToast("Erro ao verificar código: ${e.message}")
        }


        if (code_valid){
            return true
        } else {
            return false
        }

    }

    // Esta anotação é OBRIGATÓRIA para segurança
    @android.webkit.JavascriptInterface
    fun Login() {
        //android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_SHORT).show()
        webView.post {
            webView.loadUrl("file:///android_asset/carteira.html")
        }
    }

    /**
     * Método chamado pelo Javascript para pegar os contratos.
     * Retorna a String JSON gerada pela classe DadosJson.
     */
    @JavascriptInterface
    fun getListaContratos(): String {
        return dados.jsonHelper.contratosJson
    }

    /**
     * Método chamado pelo Javascript quando o usuário clica em um contrato.
     */
    @JavascriptInterface
    fun SelecionarContrato(codigoContrato: String, clienteAtivo: String) {
        // Exemplo de como você pode salvar isso internamente no banco ou Preferences
        dados.putString("CONTRATO_ATIVO", codigoContrato)
        dados.putString("CLIENTE_ATIVO", clienteAtivo)
        Log.i("Contrato selecionado " + codigoContrato, codigoContrato)
        // Navega para a home. Como altera a UI, precisa rodar na thread principal
      //  webView.post {
      //      webView.loadUrl("file:///android_asset/contrato.html")
      //  }
    }

    //home.html
    @android.webkit.JavascriptInterface
    fun carrega_selecao() {
        var codigoContrato = dados.getString("CONTRATO_ATIVO");
        var clienteAtivo = dados.getString("CLIENTE_ATIVO");


        // 2. Precisamos rodar a chamada do JS na Thread Principal (UI Thread)
        webView.post {
            // 3. Montamos a string da função JS: preencherDadosClienteFromObj('valor1', 'valor2')
            val jsCommand = "preencherDadosCliente('$codigoContrato', '$clienteAtivo')"

            webView.evaluateJavascript(jsCommand, null)
        }
    }

    //carteira.html
    @android.webkit.JavascriptInterface
    fun carrega_carteira() {
        var codigoContrato = dados.getString("CONTRATO_ATIVO");
        var clienteAtivo = dados.getString("CLIENTE_ATIVO");
        var cli_plano ="SUPER LUXO ESPECIAL";
        var cli_cidade = "SÃO PEDRO DO PIAUÍ";


        // 2. Precisamos rodar a chamada do JS na Thread Principal (UI Thread)
        webView.post {
            // 3. Montamos a string da função JS: preencherDadosClienteFromObj('valor1', 'valor2')
            val jsCommand = "preencherDadosCliente('$codigoContrato', '$clienteAtivo', '$cli_plano', '$cli_cidade')"

            webView.evaluateJavascript(jsCommand, null)
        }
    }

    //Contrato.html
    @JavascriptInterface
    fun getDadosClienteAtivo(): String {
        val codigo = dados.getString("CONTRATO_ATIVO")
        return dados.getJsonCliente(codigo) // Esse método já existe no seu Dados.java!
    }


    @JavascriptInterface
    fun getDependentesAtivo(): String {
        val codigo = dados.getString("CONTRATO_ATIVO")
        Log.i("getDependentesAtivo","getDependentesAtivo")
        return dados.getJsonDependentes(codigo) // Esse também já existe!
    }

    @JavascriptInterface
    fun getDependentes(cliCodigo: String?): String {
        return dados.getJsonDependentes(cliCodigo)
    }



    @android.webkit.JavascriptInterface
    fun Conveniados() {
        //android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_SHORT).show()
        webView.post {
            webView.loadUrl("file:///android_asset/convenios.html")
        }
    }

    @android.webkit.JavascriptInterface
    fun Informacoes() {
        //android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_SHORT).show()
        webView.post {
            webView.loadUrl("file:///android_asset/informacoes.html")
        }
    }

    @android.webkit.JavascriptInterface
    fun Contrato() {
        webView.post {
            webView.loadUrl("file:///android_asset/contrato.html")
        }
    }

    @android.webkit.JavascriptInterface
    fun showToast(message: String) {
        android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    @android.webkit.JavascriptInterface
    fun onLoginSuccess(user: String) {
        // Você pode disparar ações do Android aqui quando o JS avisar que logou
        println("Usuário logado: $user")
    }

    // Método que o HTML do Financeiro (Parcelas) vai chamar
    @JavascriptInterface
    fun getParcelas(cliCodigo: String?): String {
        return dados.getJsonParcelas(cliCodigo)
    }

    // Método que o HTML dos Conveniados vai chamar
    @JavascriptInterface
    fun getConveniados(): String {
        return dados.getJsonConveniados()
    }

    // ==========================================
    // MÉTODOS DE LOGIN E SALVAMENTO DE SENHA
    // ==========================================

    @JavascriptInterface
    fun getSavedCpf(): String {
        return dados.getString("SAVED_CPF") ?: ""
    }

    @JavascriptInterface
    fun getSavedPassword(): String {
        return dados.getString("SAVED_PASS") ?: ""
    }

    @JavascriptInterface
    fun realizarLoginAndroid(cpf: String, pass: String, lembrar: Boolean) {
        // 1. Gera o token padrão de autorização (se a sua API exigir isso antes do login)
        apiClient.gerarToken(object : ApiClient.ApiCallback {
            override fun onSuccess(response: org.json.JSONObject) {
                lastToken = response.getString("access_token")

                // 2. Chama a API de login
                apiClient.fazerLoginApi(lastToken, cpf, pass, object : ApiClient.ApiCallback {
                    override fun onSuccess(loginResp: org.json.JSONObject) {
                        val respStatus = loginResp.optString("resp")

                        if (respStatus == "ok") {
                            // SUCESSO! Recupera a Session criada no VB6
                            val sessionToken = loginResp.optString("session")

                            // Salva a sessão no SQLite/SharedPreferences (Dados.java)
                            dados.putString("SESSION_TOKEN", sessionToken)
                            dados.putString("CPF_ATIVO", cpf) // Útil para requisições futuras

                            // Lógica do Checkbox "Lembrar Senha"
                            if (lembrar) {
                                dados.putString("SAVED_CPF", cpf)
                                dados.putString("SAVED_PASS", pass)
                            } else {
                                // Se o usuário desmarcou, apagamos do aparelho
                                dados.putString("SAVED_CPF", "")
                                dados.putString("SAVED_PASS", "")
                            }

                            sincronizarDadosDoServidor(
                                cpf = cpf,
                                accessToken = lastToken,
                                onComplete = {
                                    // Só redireciona quando TUDO for baixado e salvo no SQLite
                                    webView.post {
                                    //    if (dados.getQuantidadeClientes() >1)
                                     //   webView.loadUrl("file:///android_asset/selecao_contrato.html")
                                    //    else
                                        webView.loadUrl("file:///android_asset/contrato.html")
                                    }
                                },
                                onError = { erroMsg ->
                                    // Se der erro no download dos dados, tira o loading e mostra o erro
                                    webView.post {
                                        webView.evaluateJavascript("showLoginError('$erroMsg');", null)
                                    }
                                }
                            )

                        } else {
                            // ERRO DE SENHA OU USUÁRIO
                            val msgErro = loginResp.optString("msg", "Usuário ou senha inválidos.")
                            // Manda o HTML apagar o loading e exibir o erro vermelho
                            webView.post {
                                webView.evaluateJavascript("showLoginError('$msgErro');", null)
                            }
                        }
                    }

                    override fun onError(error: String) {
                        webView.post {
                            webView.evaluateJavascript("showLoginError('Erro de conexão ao servidor.');", null)
                        }
                    }
                })
            }

            override fun onError(error: String) {
                webView.post {
                    webView.evaluateJavascript("showLoginError('Falha de segurança ao conectar.');", null)
                }
            }
        })
    }
    /**
     * Sincroniza os dados do usuário (Cliente, Dependentes e Financeiro)
     * encadeando as requisições para garantir a ordem correta de dependência.
     */
    /**
     * Sincroniza os dados do usuário (Pode retornar múltiplos clientes/contratos)
     * Encadeia as requisições para garantir a ordem correta e aguarda todas terminarem.
     */

    /**
     * Sincroniza os dados do usuário (Pode retornar múltiplos clientes/contratos)
     * Utiliza o novo endpoint consolidado e encadeia o financeiro.
     */
    private fun sincronizarDadosDoServidor(cpf: String, accessToken: String, onComplete: () -> Unit, onError: (String) -> Unit) {
        // 1. Busca os dados completos (Clientes e Dependentes aninhados)
        apiClient.buscarDadosCompletos(accessToken, cpf, object : ApiClient.ApiCallback {
            override fun onSuccess(clienteResp: org.json.JSONObject) {
                try {
                    val respStatus = clienteResp.optString("resp")
                    if (respStatus == "er") {
                        onError(clienteResp.optString("msg", "Erro ao buscar os contratos."))
                        return
                    }

                    val dadosArray = clienteResp.optJSONArray("dados")
                    if (dadosArray == null || dadosArray.length() == 0) {
                        onError("Nenhum contrato encontrado para este CPF.")
                        return
                    }

                    val totalClientes = dadosArray.length()
                    var clientesProcessados = 0
                    var ocorreuErro = false // Flag para evitar chamar onError múltiplas vezes

                    dados.apagacliantes()

                    // Loop por cada contrato/cliente retornado no JSON
                    for (i in 0 until totalClientes) {
                        val cliJson = dadosArray.getJSONObject(i)
                        val cliCodigo = cliJson.optString("CLI_CODIGO", "")

                        if (cliCodigo.isEmpty()) {
                            clientesProcessados++
                            // Se esse for o último e estiver vazio, finaliza
                            if (clientesProcessados == totalClientes && !ocorreuErro) onComplete()
                            continue
                        }

                        // 2. Salva o cliente no banco local (Assumindo LOGIN_SEQ = 1)
                        dados.sincronizarClienteApi(1, cliJson)

                        // 3. Busca e salva os Dependentes aninhados na mesma resposta
                        val depArray = cliJson.optJSONArray("dependentes") ?: org.json.JSONArray()
                        dados.sincronizarDependentesApi(cliCodigo, depArray)

                        // 4. Busca as Parcelas Deste Cliente Específico
                        apiClient.buscarParcelas(accessToken, cliCodigo, object : ApiClient.ApiCallback {
                            override fun onSuccess(parcResp: org.json.JSONObject) {
                                if (ocorreuErro) return

                                val parcArray = parcResp.optJSONArray("dados") ?: org.json.JSONArray()
                                dados.sincronizarParcelasApi(cliCodigo, parcArray)

                                // === VERIFICAÇÃO DE CONCLUSÃO ===
                                clientesProcessados++
                                if (clientesProcessados == totalClientes && !ocorreuErro) {
                                    // Sincroniza os Conveniados por último, pois são gerais (não dependem do cliente)
                                    apiClient.buscarConveniados(accessToken, object : ApiClient.ApiCallback {
                                        override fun onSuccess(convResp: org.json.JSONObject) {
                                            val convArray = convResp.optJSONArray("dados") ?: org.json.JSONArray()
                                            dados.sincronizarConveniadosApi(convArray)
                                            onComplete() // Todos os dados baixados com sucesso!
                                        }

                                        override fun onError(error: String) {
                                            // Se der erro nos conveniados, loga, mas deixa o usuário logar mesmo assim
                                            Log.e("SYNC", "Falha ao sincronizar conveniados: $error")
                                            onComplete()
                                        }
                                    })
                                }
                            }

                            override fun onError(error: String) {
                                if (!ocorreuErro) {
                                    ocorreuErro = true
                                    onError("Falha ao sincronizar financeiro ($cliCodigo): $error")
                                }
                            }
                        })
                    }

                } catch (e: Exception) {
                    onError("Erro ao processar dados do cliente: ${e.message}")
                }
            }

            override fun onError(error: String) {
                onError("Falha ao buscar dados do cliente: $error")
            }
        })
    }
}

@Composable
fun WebViewScreen(url: String, modifier: Modifier = Modifier, activity: MainActivity, dados: Dados) {
    // 1. Criamos uma referência para a WebView que persiste durante recomposições
    // 1. IMPORTANTE: Use 'remember' para que a referência persista entre recomposições
    var webViewRef: WebView? by remember { mutableStateOf(null) }

    var mostrarDialogo by remember { mutableStateOf(false) }

    // 1. Definição do Diálogo (Estilo Compose)
    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { androidx.compose.material3.Text("Sair do App") },
            text = { androidx.compose.material3.Text("Deseja realmente fechar o aplicativo da PAX União?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { activity.finish() }) {
                    androidx.compose.material3.Text("Sim")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { mostrarDialogo = false }) {
                    androidx.compose.material3.Text("Não")
                }
            }
        )
    }

    // 2. Intercepta o botão voltar
    BackHandler(enabled = true) {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack() // Volta a página interna
        } else {
            mostrarDialogo = true // Abre o diálogo em vez de fechar o app
        }
    }



    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                webViewRef = this

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient() // Abre links dentro do próprio app

                settings.apply {
                    javaScriptEnabled = true // Ativa o JavaScript
                    domStorageEnabled = true // Útil para sites modernos
                    // Permite que o HTML carregado de um arquivo acesse outros arquivos locais
                    allowFileAccess = true
                    allowContentAccess = true
                }
                // "Android" será o nome do objeto dentro do JavaScript
                addJavascriptInterface(WebAppInterface(activity,this, dados), "Android")

                loadUrl(url)


            }

        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        }
    )
}

