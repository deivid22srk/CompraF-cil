-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create app_config table
CREATE TABLE IF NOT EXISTS app_config (
    key TEXT PRIMARY KEY,
    value JSONB NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Create profiles table
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY REFERENCES auth.users ON DELETE CASCADE,
    full_name TEXT,
    avatar_url TEXT,
    whatsapp TEXT,
    role TEXT DEFAULT 'user' NOT NULL,
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

-- Create addresses table
CREATE TABLE IF NOT EXISTS addresses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users ON DELETE CASCADE,
    name TEXT NOT NULL,
    receiver_name TEXT,
    phone TEXT NOT NULL,
    address_line TEXT NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Idempotent receiver_name addition
-- Idempotent column additions for existing tables
DO $$
BEGIN
    -- profiles.role
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='profiles' AND column_name='role') THEN
        ALTER TABLE profiles ADD COLUMN role TEXT DEFAULT 'user' NOT NULL;
    END IF;

    -- addresses.receiver_name
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='addresses' AND column_name='receiver_name') THEN
        ALTER TABLE addresses ADD COLUMN receiver_name TEXT;
    END IF;
END $$;

-- Create orders table
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users ON DELETE SET NULL,
    customer_name TEXT,
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

-- More idempotent column additions
DO $$
BEGIN
    -- orders.customer_name
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orders' AND column_name='customer_name') THEN
        ALTER TABLE orders ADD COLUMN customer_name TEXT;
    END IF;

    -- products.variations
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='products' AND column_name='variations') THEN
        ALTER TABLE products ADD COLUMN variations JSONB DEFAULT '[]'::jsonb;
    END IF;

    -- cart_items.selected_variations
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='cart_items' AND column_name='selected_variations') THEN
        ALTER TABLE cart_items ADD COLUMN selected_variations JSONB;
    END IF;

    -- order_items.selected_variations
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='order_items' AND column_name='selected_variations') THEN
        ALTER TABLE order_items ADD COLUMN selected_variations JSONB;
    END IF;

    -- profiles.email
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='profiles' AND column_name='email') THEN
        ALTER TABLE profiles ADD COLUMN email TEXT;
    END IF;

    -- profiles.permissions
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='profiles' AND column_name='permissions') THEN
        ALTER TABLE profiles ADD COLUMN permissions JSONB DEFAULT '{}'::jsonb;
    END IF;
END $$;

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
    IF NOT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'addresses' AND rowsecurity = true) THEN
        ALTER TABLE addresses ENABLE ROW LEVEL SECURITY;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'app_config' AND rowsecurity = true) THEN
        ALTER TABLE app_config ENABLE ROW LEVEL SECURITY;
    END IF;
END $$;

-- Policies
DO $$
BEGIN
    -- App Config
    DROP POLICY IF EXISTS "Allow public select on app_config" ON app_config;
    CREATE POLICY "Allow public select on app_config" ON app_config FOR SELECT USING (true);
    DROP POLICY IF EXISTS "Only admins can manage app_config" ON app_config;
    CREATE POLICY "Only admins can manage app_config" ON app_config FOR ALL USING (
        auth.role() = 'service_role' OR
        EXISTS (SELECT 1 FROM profiles WHERE profiles.id = auth.uid() AND (profiles.role = 'admin' OR profiles.role = 'main_admin'))
    );

    -- Addresses
    DROP POLICY IF EXISTS "Users can manage their own addresses" ON addresses;
    CREATE POLICY "Users can manage their own addresses" ON addresses FOR ALL USING (auth.uid() = user_id);

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
    DROP POLICY IF EXISTS "Only admins can manage categories" ON categories;
    CREATE POLICY "Only admins can manage categories" ON categories FOR ALL USING (
        EXISTS (SELECT 1 FROM profiles WHERE profiles.id = auth.uid() AND (profiles.role = 'admin' OR profiles.role = 'main_admin'))
    );

    -- Products
    DROP POLICY IF EXISTS "Allow public select on products" ON products;
    CREATE POLICY "Allow public select on products" ON products FOR SELECT USING (true);
    DROP POLICY IF EXISTS "Only admins can manage products" ON products;
    CREATE POLICY "Only admins can manage products" ON products FOR ALL USING (
        EXISTS (SELECT 1 FROM profiles WHERE profiles.id = auth.uid() AND (profiles.role = 'admin' OR profiles.role = 'main_admin'))
    );

    -- Product Images
    DROP POLICY IF EXISTS "Allow public select on product_images" ON product_images;
    CREATE POLICY "Allow public select on product_images" ON product_images FOR SELECT USING (true);
    DROP POLICY IF EXISTS "Only admins can manage product_images" ON product_images;
    CREATE POLICY "Only admins can manage product_images" ON product_images FOR ALL USING (
        EXISTS (SELECT 1 FROM profiles WHERE profiles.id = auth.uid() AND (profiles.role = 'admin' OR profiles.role = 'main_admin'))
    );

    -- Cart Items
    DROP POLICY IF EXISTS "Users can view their own cart items" ON cart_items;
    CREATE POLICY "Users can view their own cart items" ON cart_items FOR SELECT USING (auth.uid() = user_id);
    DROP POLICY IF EXISTS "Users can manage their own cart items" ON cart_items;
    CREATE POLICY "Users can manage their own cart items" ON cart_items FOR ALL USING (auth.uid() = user_id);

    -- Orders
    DROP POLICY IF EXISTS "Users can view their own orders" ON orders;
    CREATE POLICY "Users can view their own orders" ON orders FOR SELECT USING (
        auth.uid() = user_id OR
        EXISTS (SELECT 1 FROM profiles WHERE profiles.id = auth.uid() AND (profiles.role = 'admin' OR profiles.role = 'main_admin'))
    );
    DROP POLICY IF EXISTS "Authenticated users can insert orders" ON orders;
    CREATE POLICY "Authenticated users can insert orders" ON orders FOR INSERT WITH CHECK (auth.uid() = user_id);
    DROP POLICY IF EXISTS "Only admins can delete orders" ON orders;
    CREATE POLICY "Only admins can delete orders" ON orders FOR DELETE USING (
        EXISTS (SELECT 1 FROM profiles WHERE profiles.id = auth.uid() AND (profiles.role = 'admin' OR profiles.role = 'main_admin'))
    );
    DROP POLICY IF EXISTS "Only admins can update orders" ON orders;
    CREATE POLICY "Only admins can update orders" ON orders FOR UPDATE USING (
        EXISTS (SELECT 1 FROM profiles WHERE profiles.id = auth.uid() AND (profiles.role = 'admin' OR profiles.role = 'main_admin'))
    );

    -- Order Status History
    DROP POLICY IF EXISTS "Users can view history of their own orders" ON order_status_history;
    CREATE POLICY "Users can view history of their own orders" ON order_status_history FOR SELECT USING (
        EXISTS (SELECT 1 FROM orders WHERE orders.id = order_status_history.order_id AND (
            orders.user_id = auth.uid() OR
            EXISTS (SELECT 1 FROM profiles WHERE profiles.id = auth.uid() AND (profiles.role = 'admin' OR profiles.role = 'main_admin'))
        ))
    );
    DROP POLICY IF EXISTS "Only admins can insert order history" ON order_status_history;
    CREATE POLICY "Only admins can insert order history" ON order_status_history FOR INSERT WITH CHECK (
        EXISTS (SELECT 1 FROM profiles WHERE profiles.id = auth.uid() AND (profiles.role = 'admin' OR profiles.role = 'main_admin'))
    );

    -- Order Items
    DROP POLICY IF EXISTS "Users can view items of their own orders" ON order_items;
    CREATE POLICY "Users can view items of their own orders" ON order_items FOR SELECT USING (
        EXISTS (SELECT 1 FROM orders WHERE orders.id = order_items.order_id AND (
            orders.user_id = auth.uid() OR
            EXISTS (SELECT 1 FROM profiles WHERE profiles.id = auth.uid() AND (profiles.role = 'admin' OR profiles.role = 'main_admin'))
        ))
    );
    DROP POLICY IF EXISTS "Authenticated users can insert order items" ON order_items;
    CREATE POLICY "Authenticated users can insert order items" ON order_items FOR INSERT WITH CHECK (auth.role() = 'authenticated');
END $$;

-- Initial Categories (General Store)
INSERT INTO categories (name) VALUES
('Alimentos'),
('Bebidas'),
('Limpeza'),
('Higiene'),
('Outros')
ON CONFLICT (id) DO NOTHING;

-- Initial Config
INSERT INTO app_config (key, value) VALUES
('min_version', '"1.0"'),
('latest_version', '"1.0"'),
('download_url', '"https://comprafacil.ct.ws/download"'),
('delivery_fee', '0.0')
ON CONFLICT (key) DO NOTHING;

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

-- Set REPLICA IDENTITY to FULL to ensure all columns are sent in Realtime UPDATE events
-- This is critical for RLS and client-side filtering (e.g., user_id)
ALTER TABLE orders REPLICA IDENTITY FULL;
ALTER TABLE order_status_history REPLICA IDENTITY FULL;

-- Trigger to auto-sync email from auth.users to public.profiles
CREATE OR REPLACE FUNCTION public.handle_new_user_profile()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, email, full_name, avatar_url, role)
    VALUES (
        NEW.id,
        NEW.email,
        NEW.raw_user_meta_data->>'full_name',
        NEW.raw_user_meta_data->>'avatar_url',
        'user'
    )
    ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger for existing users updates
CREATE OR REPLACE FUNCTION public.handle_user_update_profile()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE public.profiles
    SET email = NEW.email
    WHERE id = NEW.id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user_profile();

DROP TRIGGER IF EXISTS on_auth_user_updated ON auth.users;
CREATE TRIGGER on_auth_user_updated
    AFTER UPDATE OF email ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_user_update_profile();
