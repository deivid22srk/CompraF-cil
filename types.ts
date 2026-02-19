
export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  original_price?: number;
  category: string;
  image_url: string;
  rating: number;
  rating_count: number;
  created_at?: string;
}

export interface Order {
  id: string;
  user_id: string;
  user_email: string;
  product_names: string;
  total_price: number;
  status: string;
  tracking_code?: string;
  last_location?: string;
  created_at: string;
}

export interface CartItem extends Product {
  quantity: number;
}

export type View = 'welcome' | 'home' | 'details' | 'cart' | 'admin' | 'auth' | 'search' | 'profile';

export enum Category {
  Sneakers = 'Sneakers',
  Watches = 'Watches',
  Electronics = 'Electronics',
  Apparel = 'Apparel'
}
