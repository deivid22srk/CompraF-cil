import { supabase } from '../supabaseClient';
import type { Product, ProductImage } from '../types/database';

export const productService = {
  async getProducts() {
    const { data, error } = await supabase
      .from('products')
      .select('*');
    if (error) throw error;
    return data as Product[];
  },

  async getProductById(id: string) {
    const { data, error } = await supabase
      .from('products')
      .select('*')
      .eq('id', id)
      .single();
    if (error) throw error;
    return data as Product;
  },

  async getProductImages(productId: string) {
    const { data, error } = await supabase
      .from('product_images')
      .select('*')
      .eq('product_id', productId);
    if (error) throw error;
    return data as ProductImage[];
  }
};
