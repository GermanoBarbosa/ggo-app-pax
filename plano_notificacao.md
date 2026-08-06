# Plano para implementar mensagens via Firebase

## 1. Adicionar dependência FCM

No `app/build.gradle.kts`:

```kotlin
implementation("com.google.firebase:firebase-messaging-ktx:24.1.0")
```

## 2. Criar um FirebaseMessagingService

Uma classe que estende `FirebaseMessagingService` para:

- **`onNewToken()`** — receber o token FCM e enviá-lo ao seu backend
- **`onMessageReceived()`** — processar mensagens recebidas e exibir notificações

Arquivo: `app/src/main/java/br/com/paxuniao/app/MyFirebaseMessagingService.kt`

E um helper de notificação em `app/src/main/java/br/com/paxuniao/app/NotificationHelper.kt`.

## 3. Configurar notificações no Android

- Criar `NotificationChannel` (Android 8+)
- Solicitar permissão `POST_NOTIFICATIONS` (Android 13+)
- Registrar o service no `AndroidManifest.xml`
- Adicionar `<intent-filter>` e permissões necessárias

## 4. Vincular token FCM ao usuário logado

No momento do login, após o usuário se autenticar via sua API customizada:

1. Obter o token FCM com `FirebaseMessaging.getInstance().token`
2. Enviar para um endpoint da sua API (ex: `POST /app/register-device`) associando ao `SESSION_TOKEN` ou CPF

## 5. Enviar a mensagem

Do seu **backend (VB6)**:

- Usar a Firebase Admin SDK ou a API REST do FCM para enviar a mensagem
- Endpoint: `https://fcm.googleapis.com/fcm/send`
- Alvo: o token FCM registrado para aquele usuário

### Ou usar mensagem de dados (data message) em vez de notificação

Se quiser que o app processe a mensagem em vez do sistema mostrar uma notificação automática, use payload `data` (não `notification`). Isso permite tratar no `onMessageReceived` e exibir dentro do WebView.

## 6. Exibir a mensagem no app

- **Notificação push normal**: o sistema Android exibe automaticamente
- **Data message**: usar a bridge `JavascriptInterface` para passar a mensagem para o HTML/JS do WebView e exibir lá dentro
