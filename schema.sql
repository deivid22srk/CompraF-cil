-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create profiles table
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY REFERENCES auth.users ON DELETE CASCADE,
    full_name TEXT,
    avatar_url TEXT,
    whatsapp TEXT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Create categories table
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    icon_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Create products table
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    image_url TEXT,
    stock_quantity INTEGER DEFAULT 0,
    sold_by TEXT,
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Create product images table for multiple images
CREATE TABLE IF NOT EXISTS product_images (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID REFERENCES products(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Create cart_items table
CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE CASCADE,
    quantity INTEGER DEFAULT 1 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Create orders table
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users ON DELETE SET NULL,
    whatsapp TEXT NOT NULL,
    location TEXT NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    payment_method TEXT DEFAULT 'dinheiro' NOT NULL,
    status TEXT DEFAULT 'pendente' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Create order status history table
CREATE TABLE IF NOT EXISTS order_status_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID REFERENCES orders(id) ON DELETE CASCADE,
    status TEXT NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Create order items table
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id),
    quantity INTEGER NOT NULL,
    price_at_time DECIMAL(10, 2) NOT NULL
);

-- Enable RLS
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'profiles' AND rowsecurity = true) THEN
        ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'categories' AND rowsecurity = true) THEN
        ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'products' AND rowsecurity = true) THEN
        ALTER TABLE products ENABLE ROW LEVEL SECURITY;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'product_images' AND rowsecurity = true) THEN
        ALTER TABLE product_images ENABLE ROW LEVEL SECURITY;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'cart_items' AND rowsecurity = true) THEN
        ALTER TABLE cart_items ENABLE ROW LEVEL SECURITY;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'orders' AND rowsecurity = true) THEN
        ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'order_status_history' AND rowsecurity = true) THEN
        ALTER TABLE order_status_history ENABLE ROW LEVEL SECURITY;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'order_items' AND rowsecurity = true) THEN
        ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;
    END IF;
END $$;

-- Policies
DO $$
BEGIN
    -- Profiles
    DROP POLICY IF EXISTS "Public profiles are viewable by everyone" ON profiles;
    CREATE POLICY "Public profiles are viewable by everyone" ON profiles FOR SELECT USING (true);
    DROP POLICY IF EXISTS "Users can insert their own profile" ON profiles;
    CREATE POLICY "Users can insert their own profile" ON profiles FOR INSERT WITH CHECK (auth.uid() = id);
    DROP POLICY IF EXISTS "Users can update own profile" ON profiles;
    CREATE POLICY "Users can update own profile" ON profiles FOR UPDATE USING (auth.uid() = id);

    -- Categories
    DROP POLICY IF EXISTS "Allow public select on categories" ON categories;
    CREATE POLICY "Allow public select on categories" ON categories FOR SELECT USING (true);
    DROP POLICY IF EXISTS "Allow anon/auth all on categories" ON categories;
    CREATE POLICY "Allow anon/auth all on categories" ON categories FOR ALL USING (true) WITH CHECK (true);

    -- Products
    DROP POLICY IF EXISTS "Allow public select on products" ON products;
    CREATE POLICY "Allow public select on products" ON products FOR SELECT USING (true);
    DROP POLICY IF EXISTS "Allow anon/auth all on products" ON products;
    CREATE POLICY "Allow anon/auth all on products" ON products FOR ALL USING (true) WITH CHECK (true);

    -- Product Images
    DROP POLICY IF EXISTS "Allow public select on product_images" ON product_images;
    CREATE POLICY "Allow public select on product_images" ON product_images FOR SELECT USING (true);
    DROP POLICY IF EXISTS "Allow anon/auth all on product_images" ON product_images;
    CREATE POLICY "Allow anon/auth all on product_images" ON product_images FOR ALL USING (true) WITH CHECK (true);

    -- Cart Items
    DROP POLICY IF EXISTS "Users can view their own cart items" ON cart_items;
    CREATE POLICY "Users can view their own cart items" ON cart_items FOR SELECT USING (auth.uid() = user_id);
    DROP POLICY IF EXISTS "Users can manage their own cart items" ON cart_items;
    CREATE POLICY "Users can manage their own cart items" ON cart_items FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

    -- Orders
    DROP POLICY IF EXISTS "Users can view their own orders" ON orders;
    CREATE POLICY "Users can view their own orders" ON orders FOR SELECT USING (auth.uid() = user_id);
    DROP POLICY IF EXISTS "Anyone can insert orders" ON orders;
    CREATE POLICY "Anyone can insert orders" ON orders FOR INSERT WITH CHECK (true);
    DROP POLICY IF EXISTS "Allow public select on orders" ON orders;
    CREATE POLICY "Allow public select on orders" ON orders FOR SELECT USING (true);

    -- Order Status History
    DROP POLICY IF EXISTS "Users can view their own order history" ON order_status_history;
    CREATE POLICY "Users can view their own order history" ON order_status_history FOR SELECT USING (true); -- Simplified for now
    DROP POLICY IF EXISTS "Allow anon/auth insert on order status history" ON order_status_history;
    CREATE POLICY "Allow anon/auth insert on order status history" ON order_status_history FOR INSERT WITH CHECK (true);

    -- Order Items
    DROP POLICY IF EXISTS "Anyone can insert order items" ON order_items;
    CREATE POLICY "Anyone can insert order items" ON order_items FOR INSERT WITH CHECK (true);
    DROP POLICY IF EXISTS "Allow public select on order_items" ON order_items;
    CREATE POLICY "Allow public select on order_items" ON order_items FOR SELECT USING (true);
END $$;

-- Initial Categories (General Store)
INSERT INTO categories (name) VALUES
('Alimentos'),
('Bebidas'),
('Limpeza'),
('Higiene'),
('Outros')
ON CONFLICT DO NOTHING;

-- Enable Realtime for orders table
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime' AND tablename = 'orders'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE orders;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime' AND tablename = 'order_status_history'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE order_status_history;
    END IF;
END $$;
