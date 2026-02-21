package br.com.paxuniao.app

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import br.com.paxuniao.app.ui.theme.ClientesTheme


class MainActivity : ComponentActivity() {

    private lateinit var dados: Dados

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dados = Dados()
        dados.open(this)
        val versao = dados.getDBVersion()
        Toast.makeText(this, "DB Version $versao", Toast.LENGTH_SHORT).show()

        enableEdgeToEdge()
        setContent {
            ClientesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Chamada da WebView passando o preenchimento do Scaffold
                    WebViewScreen(
                        url = "file:///android_asset/login.html",  // Substitua pela sua URL
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

    // Esta anotação é OBRIGATÓRIA para segurança
    @android.webkit.JavascriptInterface
    fun Login() {
        //android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_SHORT).show()
        webView.post {
            webView.loadUrl("file:///android_asset/carteira.html")
        }
    }

    @android.webkit.JavascriptInterface
    fun Conveniados() {
        //android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_SHORT).show()
        webView.post {
            webView.loadUrl("file:///android_asset/Conveniados.html")
        }
    }

    @android.webkit.JavascriptInterface
    fun Informacoes() {
        //android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_SHORT).show()
        webView.post {
            webView.loadUrl("file:///android_asset/Informacoes.html")
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
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
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
