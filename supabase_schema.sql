-- Create a table for categories
CREATE TABLE categories (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Insert some default categories
INSERT INTO categories (name) VALUES
  ('Alimentos'),
  ('Eletrônicos'),
  ('Roupas'),
  ('Casa'),
  ('Beleza');

-- Create a table for products
CREATE TABLE products (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  price DECIMAL(10, 2) NOT NULL,
  image_url TEXT,
  category_id UUID REFERENCES categories(id),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Set up Row Level Security (RLS)
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;

-- IMPORTANT: This allows anyone with the anon key to read/write.
-- For production, change this to "auth.role() = 'authenticated'".
CREATE POLICY "Public full access on products" ON products
  FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Public full access on categories" ON categories
  FOR ALL USING (true) WITH CHECK (true);

-- Create a storage bucket for product images
INSERT INTO storage.buckets (id, name, public) VALUES ('product-images', 'product-images', true);

CREATE POLICY "Public Access" ON storage.objects FOR SELECT USING (bucket_id = 'product-images');
CREATE POLICY "Public Upload" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'product-images');
CREATE POLICY "Public Update" ON storage.objects FOR UPDATE USING (bucket_id = 'product-images');
CREATE POLICY "Public Delete" ON storage.objects FOR DELETE USING (bucket_id = 'product-images');
