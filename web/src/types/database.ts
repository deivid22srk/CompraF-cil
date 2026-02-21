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
