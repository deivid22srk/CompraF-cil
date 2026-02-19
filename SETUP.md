# Configuração do CompraFácil

Para que o login com Google funcione corretamente, você precisa configurar seu projeto no Supabase e no Google Cloud Console.

## 1. Habilitar o Provedor Google no Supabase

1. Vá para o [Painel do Supabase](https://supabase.com/dashboard).
2. Selecione seu projeto.
3. No menu lateral, vá em **Authentication** > **Providers**.
4. Encontre **Google** na lista e clique para expandir.
5. Alterne a chave para **Enabled**.
6. Você verá campos para `Client ID` e `Client Secret`. Você precisará obtê-los no próximo passo.

## 2. Configurar o Google Cloud Console

1. Vá para o [Google Cloud Console](https://console.cloud.google.com/).
2. Crie um novo projeto ou selecione um existente.
3. Vá em **APIs & Services** > **OAuth consent screen**.
4. Configure a tela de consentimento (escolha "External" se não for uma organização).
5. Vá em **APIs & Services** > **Credentials**.
6. Clique em **+ CREATE CREDENTIALS** > **OAuth client ID**.
7. Selecione **Web application** (mesmo sendo para Android, o Supabase usa o fluxo Web para OAuth).
8. Em **Authorized redirect URIs**, adicione a URL que aparece no painel do Supabase (geralmente algo como `https://zlykhkpycrsukoaxhfzn.supabase.co/auth/v1/callback`).
9. Clique em **Create**.
10. Copie o **Client ID** e o **Client Secret** e cole-os nas configurações do Google no painel do Supabase (passo 1).

## 3. Configurar Redirect URLs no Supabase

Ainda no painel do Supabase em **Authentication** > **URL Configuration**:

1. Em **Site URL**, você pode colocar o domínio do seu site ou `http://localhost:3000`.
2. Em **Redirect URLs**, adicione os esquemas personalizados dos aplicativos:
   - `comprafacil://login-callback` (Para o app do usuário)
   - `comprafacil-admin://login-callback` (Para o app do admin)

## 4. Executar o Script SQL

Certifique-se de ter executado o conteúdo do arquivo `supabase_schema.sql` no **SQL Editor** do Supabase para criar as tabelas e políticas de segurança necessárias.

---

Após seguir esses passos, o erro "Unsupported provider" deve desaparecer e o login funcionará corretamente.
