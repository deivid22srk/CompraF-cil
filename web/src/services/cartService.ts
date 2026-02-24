import { supabase } from '../supabaseClient'
import type { CartItem } from '../types/database'

export const cartService = {
  async getCartItems(userId: string) {
    const { data, error } = await supabase
      .from('cart_items')
      .select('*, product:products(*)')
      .eq('user_id', userId)

    if (error) throw error
    return data as CartItem[]
  },

  async addToCart(item: CartItem) {
    // If variations differ, they are considered different items usually,
    // but the app logic compares selected_variations.
    // Let's simplify for web: if productId and variations match.

    const { data: allSameProduct } = await supabase
        .from('cart_items')
        .select('*')
        .eq('user_id', item.user_id)
        .eq('product_id', item.product_id)

    const existingMatch = allSameProduct?.find(i =>
        JSON.stringify(i.selected_variations) === JSON.stringify(item.selected_variations)
    )

    if (existingMatch) {
      const { data, error } = await supabase
        .from('cart_items')
        .update({ quantity: existingMatch.quantity + item.quantity })
        .eq('id', existingMatch.id)
        .select()
        .single()
      if (error) throw error
      return data
    } else {
      const { data, error } = await supabase
        .from('cart_items')
        .insert(item)
        .select()
        .single()
      if (error) throw error
      return data
    }
  },

  async updateQuantity(itemId: string, quantity: number) {
    const { data, error } = await supabase
      .from('cart_items')
      .update({ quantity })
      .eq('id', itemId)
      .select()
      .single()

    if (error) throw error
    return data
  },

  async removeItem(itemId: string) {
    const { error } = await supabase
      .from('cart_items')
      .delete()
      .eq('id', itemId)

    if (error) throw error
  },

  async clearCart(userId: string) {
    const { error } = await supabase
      .from('cart_items')
      .delete()
      .eq('user_id', userId)

    if (error) throw error
  }
}
