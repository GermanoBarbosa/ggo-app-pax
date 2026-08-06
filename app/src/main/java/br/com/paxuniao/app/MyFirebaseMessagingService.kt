package br.com.paxuniao.app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i("FCM", "Novo token FCM recebido")

        val dados = Dados()
        dados.open(applicationContext)
        dados.putString("FCM_TOKEN", token)

        FcmRegistro.registrar(applicationContext)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val titulo = remoteMessage.data["titulo"]
            ?: remoteMessage.notification?.title
            ?: "Aviso"
        val mensagem = remoteMessage.data["mensagem"]
            ?: remoteMessage.notification?.body

        if (mensagem.isNullOrBlank()) {
            Log.i("FCM", "Mensagem sem conteúdo, ignorada")
            return
        }

        if (MainActivity.appEmPrimeiroPlano) {
            MainActivity.enviarMensagemParaWebView(titulo, mensagem)
        } else {
            NotificationHelper.exibir(applicationContext, titulo, mensagem)
        }
    }
}
