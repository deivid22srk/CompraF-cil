import { supabase } from '../supabaseClient'
import type { Order, OrderItem } from '../types/database'

export const orderService = {
  async placeOrder(order: Order, items: OrderItem[]) {
    // 1. Insert Order
    const { data: insertedOrder, error: orderError } = await supabase
      .from('orders')
      .insert(order)
      .select()
      .single()

    if (orderError) throw orderError

    // 2. Insert Order Items
    const orderItemsWithId = items.map(item => ({
      ...item,
      order_id: insertedOrder.id
    }))

    const { error: itemsError } = await supabase
      .from('order_items')
      .insert(orderItemsWithId)

    if (itemsError) throw itemsError

    return insertedOrder
  },

  async getOrders(userId: string) {
    const { data, error } = await supabase
      .from('orders')
      .select('*')
      .eq('user_id', userId)
      .order('created_at', { ascending: false })

    if (error) throw error
    return data as Order[]
  },

  async getOrderItems(orderId: string) {
    const { data, error } = await supabase
      .from('order_items')
      .select('*, product:products(*)')
      .eq('order_id', orderId)

    if (error) throw error
    return data
  }
}
