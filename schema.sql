-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create categories table
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Create products table
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    image_url TEXT,
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Enable RLS
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE products ENABLE ROW LEVEL SECURITY;

-- Policies
DROP POLICY IF EXISTS "Allow public read for categories" ON categories;
DROP POLICY IF EXISTS "Allow public read for products" ON products;
DROP POLICY IF EXISTS "Allow all actions for anonymous users on categories" ON categories;
DROP POLICY IF EXISTS "Allow all actions for anonymous users on products" ON products;

-- Allow public (including anonymous) access for ALL operations for this demo
CREATE POLICY "Allow all actions for anonymous users on categories" ON categories FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all actions for anonymous users on products" ON products FOR ALL USING (true) WITH CHECK (true);

-- Insert some initial categories if they don't exist
INSERT INTO categories (name)
SELECT 'Eletrônicos' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Eletrônicos');
INSERT INTO categories (name)
SELECT 'Roupas' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Roupas');
INSERT INTO categories (name)
SELECT 'Alimentos' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Alimentos');
INSERT INTO categories (name)
SELECT 'Casa' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Casa');
