package br.com.paxuniao.app;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ApiClient {

    private static final String BASE_URL = Parametros.BASE_URL;
    private static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    private OkHttpClient client;
    private Handler mainHandler;

    public ApiClient() {
        client = new OkHttpClient();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    // ===============================
    // INTERFACE DE CALLBACK
    // ===============================
    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    // ===============================
    // GERAR TOKEN
    // ===============================
    public void gerarToken(String appKey, String authorization, ApiCallback callback) {

        try {
            JSONObject json = new JSONObject();
            json.put("app-key", appKey);
            json.put("authorization", authorization);

            RequestBody body = RequestBody.create(json.toString(), JSON);

            Request request = new Request.Builder()
                    .url(BASE_URL + "/oauth/token")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new DefaultCallback(callback));

        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // ===============================
    // /app/recupera
    // ===============================
    public void recuperar(String accessToken, String cpf, ApiCallback callback) {

        try {
            JSONObject json = new JSONObject();
            json.put("access_token", accessToken);
            json.put("cpf", cpf);

            RequestBody body = RequestBody.create(json.toString(), JSON);

            Request request = new Request.Builder()
                    .url(BASE_URL + "/app/recupera")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new DefaultCallback(callback));

        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // ===============================
    // /app/recupera_email
    // ===============================
    public void recuperarEmail(String cpfLimpo, String accessToken, String email, ApiCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("access_token", accessToken); // O token gerado previamente
            json.put("email", email); // O e-mail selecionado pelo usuário
            json.put("cpf", cpfLimpo);

            RequestBody body = RequestBody.create(json.toString(), JSON);

            Request request = new Request.Builder()
                    .url(BASE_URL + "/app/recupera/email")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new DefaultCallback(callback));
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // ===============================
    // /app/recupera_fone
    // ===============================
    public void recuperarFone(String cpfLimpo, String accessToken, String fone, ApiCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("access_token", accessToken); // O token gerado previamente
            json.put("fone", fone); // O telefone selecionado, contendo DDD + Número
            json.put("cpf", cpfLimpo);

            RequestBody body = RequestBody.create(json.toString(), JSON);

            Request request = new Request.Builder()
                    .url(BASE_URL + "/app/recupera/fone")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new DefaultCallback(callback));
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // ===============================
    // CALLBACK PADRÃO
    // ===============================
    private class DefaultCallback implements Callback {

        private ApiCallback callback;

        DefaultCallback(ApiCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            mainHandler.post(() ->
                    callback.onError(e.getMessage())
            );
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {

            String responseBody = response.body().string();

            mainHandler.post(() -> {
                try {
                    JSONObject json = new JSONObject(responseBody);
                    callback.onSuccess(json);
                } catch (Exception e) {
                    callback.onError("Erro ao converter JSON: " + e.getMessage());
                }
            });
        }
    }
}