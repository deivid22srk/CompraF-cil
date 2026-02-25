-- SQL Schema for new features in CompraFacil

-- 1. Addresses Table
CREATE TABLE IF NOT EXISTS public.addresses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL, -- e.g., 'Home', 'Work'
    receiver_name TEXT,
    phone TEXT NOT NULL,
    address_line TEXT NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable RLS for addresses
ALTER TABLE public.addresses ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can manage their own addresses" ON public.addresses
    FOR ALL USING (auth.uid() = user_id);

-- 2. Order Status History Table
CREATE TABLE IF NOT EXISTS public.order_status_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID REFERENCES public.orders(id) ON DELETE CASCADE,
    status TEXT NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable RLS for order_status_history
ALTER TABLE public.order_status_history ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can view history of their own orders" ON public.order_status_history
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.orders
            WHERE public.orders.id = order_id AND public.orders.user_id = auth.uid()
        )
    );
CREATE POLICY "Admins can manage all order history" ON public.order_status_history
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM public.profiles
            WHERE public.profiles.id = auth.uid() AND public.profiles.role = 'admin'
        )
    );

-- 3. Update Orders table with coordinates
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- 4. App Config Table (for Admin settings)
CREATE TABLE IF NOT EXISTS public.app_config (
    key TEXT PRIMARY KEY,
    value JSONB NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Insert default delivery fee
INSERT INTO public.app_config (key, value) VALUES ('delivery_fee', '{"amount": 5.0}') ON CONFLICT DO NOTHING;

-- Enable RLS for app_config
ALTER TABLE public.app_config ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can read app_config" ON public.app_config FOR SELECT USING (true);
CREATE POLICY "Admins can update app_config" ON public.app_config FOR ALL USING (
    EXISTS (
        SELECT 1 FROM public.profiles
        WHERE public.profiles.id = auth.uid() AND public.profiles.role = 'admin'
    )
);
