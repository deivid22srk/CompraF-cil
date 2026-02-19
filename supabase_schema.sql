-- Create categories table
CREATE TABLE categories (
  id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
  name text NOT NULL,
  image_url text,
  created_at timestamp with time zone DEFAULT now()
);

-- Create products table
CREATE TABLE products (
  id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
  name text NOT NULL,
  description text,
  price numeric NOT NULL,
  image_url text,
  category_id uuid REFERENCES categories(id) ON DELETE SET NULL,
  created_at timestamp with time zone DEFAULT now()
);

-- Set up Row Level Security (RLS)
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE products ENABLE ROW LEVEL SECURITY;

-- Create policies for categories
CREATE POLICY "Allow public read access on categories" ON categories FOR SELECT USING (true);
CREATE POLICY "Allow authenticated insert/update/delete on categories" ON categories FOR ALL USING (auth.role() = 'authenticated');

-- Create policies for products
CREATE POLICY "Allow public read access on products" ON products FOR SELECT USING (true);
CREATE POLICY "Allow authenticated insert/update/delete on products" ON products FOR ALL USING (auth.role() = 'authenticated');

-- Note: You should create a storage bucket named 'products' in the Supabase Dashboard.
-- And set its privacy to 'Public'.
-- Or run the following if you have enough permissions:
-- INSERT INTO storage.buckets (id, name, public) VALUES ('products', 'products', true);
-- CREATE POLICY "Public Access" ON storage.objects FOR SELECT USING (bucket_id = 'products');
-- CREATE POLICY "Authenticated Upload" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'products' AND auth.role() = 'authenticated');
