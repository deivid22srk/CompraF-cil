# CompraFácil

CompraFácil é um sistema completo de e-commerce composto por dois aplicativos Android desenvolvidos em Kotlin com Jetpack Compose e integrados ao Supabase.

## 📱 Aplicativos

1.  **CompraFacil (Loja):** Aplicativo voltado para o cliente final, permitindo visualizar o catálogo de produtos, ver detalhes e fazer login com Google.
2.  **CompraFacilAdmin (Painel):** Aplicativo administrativo para criação de produtos, upload de fotos e gerenciamento da loja.

## 🛠️ Tecnologias

- **Kotlin** & **Jetpack Compose**
- **Supabase** (Database, Authentication & Storage)
- **Material 3** (Design System)
- **GitHub Actions** (CI/CD para geração de APKs)

## 🚀 Configuração Necessária

**IMPORTANTE:** Para que o login com Google funcione, você deve configurar seu projeto no Supabase.

Siga as instruções detalhadas no arquivo:
👉 [**SETUP.md - Guia de Configuração**](SETUP.md)

## 🗄️ Banco de Dados

O arquivo `supabase_schema.sql` contém a estrutura das tabelas e as políticas de segurança (RLS) que devem ser aplicadas no seu projeto Supabase.

## 👷 CI/CD

O projeto está configurado com GitHub Actions para compilar os APKs automaticamente em cada push para a branch `main`. Os APKs podem ser encontrados na seção **Actions** do repositório após o término do build.
