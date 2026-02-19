# CompraFácil - E-commerce Regional

Este repositório contém dois aplicativos Android desenvolvidos em Kotlin e Jetpack Compose, utilizando o Supabase como backend.

## 📱 Aplicativos

1. **CompraFacil (Loja do Usuário):**
   - Design moderno e intuitivo (Azul e Amarelo).
   - Navegação por categorias.
   - Listagem de produtos em grid.
   - Detalhes do produto com descrição e fotos.
   - Autenticação via Email/Senha.

2. **CompraFacilAdmin (Painel Administrativo):**
   - Gestão de estoque.
   - Criação de novos produtos.
   - Upload de fotos diretamente para o Supabase Storage.
   - Login restrito para administradores.

## 🛠 Tecnologias
- **Kotlin & Jetpack Compose**
- **Supabase** (Auth, Postgrest, Storage)
- **Coil** (Carregamento de imagens)
- **Navigation Compose**
- **GitHub Actions** (CI/CD)

## 🚀 Como começar
Veja o arquivo [SETUP.md](SETUP.md) para instruções detalhadas de configuração do Supabase.

## 🛠 CI/CD
O projeto possui integração contínua via GitHub Actions. Toda vez que houver um push para a branch `main`, os APKs de ambos os apps serão gerados automaticamente.
