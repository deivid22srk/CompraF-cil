export interface Product {
  id: string;
  name: string;
  price: number;
  description: string;
  image_url: string;
  sold_by?: string;
  stock_quantity?: number;
  category_id?: string;
  created_at?: string;
  variations?: { name: string; values: string[] }[];
}

export interface ProductImage {
  id: string;
  product_id: string;
  image_url: string;
  created_at?: string;
}

export interface Category {
  id: string;
  name: string;
  icon_url?: string;
  created_at?: string;
}

export interface AppConfig {
  key: string;
  value: any;
  updated_at?: string;
}

export interface Profile {
  id: string;
  full_name?: string;
  avatar_url?: string;
  whatsapp?: string;
  role: string;
  updated_at?: string;
}

export interface CartItem {
  id?: string;
  user_id: string;
  product_id: string;
  quantity: number;
  selected_variations?: Record<string, string> | null;
  created_at?: string;
  product?: Product;
}

export interface Order {
  id?: string;
  user_id: string | null;
  customer_name: string | null;
  whatsapp: string;
  location: string;
  total_price: number;
  latitude: number | null;
  longitude: number | null;
  payment_method: string;
  status: string;
  created_at?: string;
}

export interface OrderItem {
  id?: string;
  order_id: string;
  product_id: string;
  quantity: number;
  price_at_time: number;
  selected_variations?: Record<string, string> | null;
}

export interface Address {
  id: string;
  user_id: string;
  name: string;
  receiver_name?: string;
  phone: string;
  address_line: string;
  latitude: number | null;
  longitude: number | null;
  created_at?: string;
}
