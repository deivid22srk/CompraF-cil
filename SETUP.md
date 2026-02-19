# Configuração do CompraFácil

Para que o sistema funcione corretamente, você precisa configurar seu projeto no Supabase.

## 1. Habilitar Autenticação por Email e Senha

1. Vá para o [Painel do Supabase](https://supabase.com/dashboard).
2. Selecione seu projeto.
3. No menu lateral, vá em **Authentication** > **Providers**.
4. Encontre **Email** na lista e clique para expandir.
5. Certifique-se de que está **Enabled**.
6. (Opcional) Desative "Confirm email" se quiser que os usuários entrem imediatamente sem confirmar o email (bom para testes).

## 2. Configurar Redirect URLs no Supabase (Para recuperação de senha, se habilitado)

Ainda no painel do Supabase em **Authentication** > **URL Configuration**:

1. Em **Redirect URLs**, adicione os esquemas personalizados dos aplicativos:
   - `comprafacil://login-callback` (Para o app do usuário)
   - `comprafacil-admin://login-callback` (Para o app do admin)

## 3. Executar o Script SQL

Certifique-se de ter executado o conteúdo do arquivo `supabase_schema.sql` no **SQL Editor** do Supabase para criar as tabelas e políticas de segurança necessárias.

---

Agora o login com Email e Senha funcionará corretamente em ambos os aplicativos.
