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
import br.com.paxuniao.app.ui.theme.ClientesTheme

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
                        url = "file:///android_asset/login.html",  // Substitua pela sua URL
                        ///url = "file:///android_asset/login_verifica_codigo.html",  // Substitua pela sua URL

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
                            // Sucesso! Mostra a mensagem e redireciona o WebView para a tela de login
                            showToast("Senha alterada com sucesso!")

                            webView.post {
                                webView.loadUrl("file:///android_asset/login.html")
                            }
                        } else {
                            // Captura a mensagem de erro da API (Ex: "Código expirado" ou "Senha fraca")
                            val msgErro = setPassResp.optString("msg", "Erro ao alterar a senha.")
                            showToast(msgErro)

                            // Opcional: Avisar o JS que deu erro para tirar a tela de loading
                            // webView.post { webView.evaluateJavascript("toggleLoading(false);", null) }
                        }
                    }

                    override fun onError(error: String) {
                        showToast("Erro de conexão ao alterar senha: $error")
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

        // Navega para a home. Como altera a UI, precisa rodar na thread principal
        webView.post {
            webView.loadUrl("file:///android_asset/home.html")
        }
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
        return dados.getJsonDependentes(codigo) // Esse também já existe!
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

