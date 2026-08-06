package br.com.paxuniao.app

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

object FcmRegistro {

    fun registrar(context: Context) {
        val dados = Dados()
        dados.open(context.applicationContext)
        val cpf = dados.getString("CPF_ATIVO")
        val session = dados.getString("SESSION_TOKEN")

        if (cpf.isEmpty() || session.isEmpty()) {
            Log.i("FCM", "Sem sessão ativa, token não registrado")
            return
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Falha ao obter token FCM", task.exception)
                return@addOnCompleteListener
            }

            val fcmToken = task.result
            if (fcmToken.isNullOrEmpty()) {
                Log.w("FCM", "Token FCM vazio, registro no servidor cancelado")
                return@addOnCompleteListener
            }

            val api = ApiClient()
            api.gerarToken(object : ApiClient.ApiCallback {
                override fun onSuccess(response: JSONObject) {
                    val accessToken = response.optString("access_token")
                    api.registrarDevice(
                        accessToken,
                        session,
                        cpf,
                        fcmToken,
                        object : ApiClient.ApiCallback {
                            override fun onSuccess(response: JSONObject) {
                                Log.i("FCM", "Dispositivo registrado no servidor")
                            }

                            override fun onError(error: String) {
                                Log.w("FCM", "Falha ao registrar dispositivo: $error")
                            }
                        })
                }

                override fun onError(error: String) {
                    Log.w("FCM", "Falha ao gerar token de acesso: $error")
                }
            })
        }
    }
}
