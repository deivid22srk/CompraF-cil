
-- Tabela de Produtos (Existing)
create table if not exists public.products (
  id uuid default gen_random_uuid() primary key,
  name text not null,
  description text,
  price decimal not null,
  original_price decimal,
  category text,
  image_url text,
  rating decimal default 4.5,
  rating_count integer default 0,
  created_at timestamp with time zone default now()
);

-- Tabela de Pedidos/Rastreio
create table if not exists public.orders (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references auth.users not null,
  user_email text not null,
  product_names text not null,
  total_price decimal not null,
  status text default 'Processando' not null, -- Processando, Enviado, Em Trânsito, Entregue
  tracking_code text,
  last_location text default 'Centro de Distribuição',
  created_at timestamp with time zone default now()
);

-- Habilitar RLS
alter table public.products enable row level security;
alter table public.orders enable row level security;

-- Políticas de Produtos
create policy "Permitir leitura para todos" on public.products for select using (true);
create policy "Admin total produtos" on public.products for all using (auth.role() = 'authenticated');

-- Políticas de Pedidos
create policy "Usuários veem seus próprios pedidos" on public.orders
  for select using (auth.uid() = user_id);

create policy "Admins veem todos os pedidos" on public.orders
  for select using (auth.role() = 'authenticated');

create policy "Admins editam pedidos" on public.orders
  for update using (auth.role() = 'authenticated');

create policy "Usuários criam pedidos" on public.orders
  for insert with check (auth.role() = 'authenticated');
