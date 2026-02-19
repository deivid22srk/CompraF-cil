# Configuração do Projeto CompraFácil

Este projeto utiliza **Supabase** para Autenticação, Banco de Dados e Armazenamento.

## 1. Supabase SQL Schema
Execute o conteúdo do arquivo `supabase_schema.sql` no **SQL Editor** do seu painel Supabase. Isso criará as tabelas `categories` e `products`, além das políticas de segurança (RLS).

## 2. Supabase Storage
No painel do Supabase:
1. Vá para **Storage**.
2. Crie um novo bucket chamado `product-images`.
3. Defina-o como **Public** (Público).

## 3. Autenticação (Email/Senha)
O app está configurado para usar **Email e Senha**.
1. Vá para **Authentication** -> **Providers**.
2. Certifique-se de que o provider **Email** está habilitado.
3. Desabilite "Confirm Email" se desejar testar rapidamente sem precisar validar o email.

## 4. Inserindo Categorias (Opcional)
Você pode inserir algumas categorias iniciais para o app do usuário exibir ícones corretamente:
```sql
INSERT INTO categories (name) VALUES ('Smartphones'), ('Vestuário'), ('Esportes'), ('Relógios');
```

## 5. Configuração do App
As chaves do Supabase já estão configuradas no arquivo `Supabase.kt` de ambos os apps:
- URL: `https://zlykhkpycrsukoaxhfzn.supabase.co`
- API Key: `sb_publishable_...`

## 6. Build
Para gerar os APKs localmente, execute:
```bash
./gradlew assembleDebug
```
Os APKs estarão em:
- `CompraFacil/build/outputs/apk/debug/CompraFacil-debug.apk`
- `CompraFacilAdmin/build/outputs/apk/debug/CompraFacilAdmin-debug.apk`
