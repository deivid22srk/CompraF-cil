import { useEffect, useState } from 'react'
import { supabase } from '../supabaseClient'
import { authService } from '../services/authService'
import { orderService } from '../services/orderService'
import type { Order } from '../types/database'
import { Package, Clock, CheckCircle2, XCircle, ChevronRight, ShoppingBag, MapPin } from 'lucide-react'
import { format } from 'date-fns'
import { ptBR } from 'date-fns/locale'

export default function Orders() {
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [userId, setUserId] = useState<string | null>(null)

  useEffect(() => {
    const checkUser = async () => {
      const user = await authService.getCurrentUser()
      if (user) {
        setUserId(user.id)
        fetchOrders(user.id)
      } else {
        setLoading(false)
      }
    }
    checkUser()
  }, [])

  useEffect(() => {
    if (!userId) return

    const channel = supabase
      .channel('orders-status')
      .on(
        'postgres_changes',
        {
          event: 'UPDATE',
          schema: 'public',
          table: 'orders',
          filter: `user_id=eq.${userId}`
        },
        (payload) => {
          const updatedOrder = payload.new as Order
          setOrders(prev => prev.map(o => o.id === updatedOrder.id ? updatedOrder : o))
        }
      )
      .subscribe()

    return () => {
      supabase.removeChannel(channel)
    }
  }, [userId])

  const fetchOrders = async (uid: string) => {
    try {
      const data = await orderService.getOrders(uid)
      setOrders(data)
    } catch (error) {
      console.error('Error fetching orders:', error)
    } finally {
      setLoading(false)
    }
  }

  const getStatusInfo = (status: string) => {
    switch (status) {
      case 'pendente':
        return { label: 'Pendente', icon: <Clock size={16} />, color: 'text-orange-500', bg: 'bg-orange-500/10' }
      case 'confirmado':
        return { label: 'Confirmado', icon: <Package size={16} />, color: 'text-blue-500', bg: 'bg-blue-500/10' }
      case 'em_preparo':
        return { label: 'Em Preparo', icon: <Clock size={16} />, color: 'text-yellow-500', bg: 'bg-yellow-500/10' }
      case 'saiu_para_entrega':
        return { label: 'Saiu para Entrega', icon: <MapPin size={16} />, color: 'text-purple-500', bg: 'bg-purple-500/10' }
      case 'entregue':
        return { label: 'Entregue', icon: <CheckCircle2 size={16} />, color: 'text-green-500', bg: 'bg-green-500/10' }
      case 'cancelado':
        return { label: 'Cancelado', icon: <XCircle size={16} />, color: 'text-red-500', bg: 'bg-red-500/10' }
      default:
        return { label: status, icon: <Clock size={16} />, color: 'text-gray-500', bg: 'bg-gray-500/10' }
    }
  }

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  return (
    <div className="flex-1 max-w-7xl mx-auto w-full px-4 py-8">
      <div className="flex items-center gap-4 mb-8">
        <div className="w-12 h-12 bg-primary/20 rounded-2xl flex items-center justify-center text-primary">
          <ShoppingBag size={24} />
        </div>
        <div>
          <h1 className="text-3xl font-black uppercase tracking-tight">Meus Pedidos</h1>
          <p className="text-gray-500 font-bold">Acompanhe o status das suas compras</p>
        </div>
      </div>

      {orders.length === 0 ? (
        <div className="bg-card rounded-3xl p-12 text-center border border-white/5">
          <div className="w-20 h-20 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-6">
            <ShoppingBag className="text-gray-500" size={32} />
          </div>
          <h2 className="text-xl font-bold mb-2">Você ainda não fez nenhum pedido</h2>
          <p className="text-gray-500 mb-8 max-w-xs mx-auto">
            Que tal dar uma olhada nas nossas ofertas e começar a comprar agora mesmo?
          </p>
          <a
            href="/"
            className="inline-flex items-center gap-2 bg-primary text-white font-black px-8 py-4 rounded-2xl hover:bg-orange-600 transition-colors uppercase tracking-widest text-xs"
          >
            Ver Produtos
          </a>
        </div>
      ) : (
        <div className="grid gap-4">
          {orders.map((order) => {
            const status = getStatusInfo(order.status)
            return (
              <div
                key={order.id}
                className="group bg-card hover:bg-white/[0.03] rounded-3xl p-6 border border-white/5 transition-all"
              >
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
                  <div className="flex items-start gap-4">
                    <div className={`w-12 h-12 ${status.bg} ${status.color} rounded-2xl flex items-center justify-center shrink-0`}>
                      {status.icon}
                    </div>
                    <div>
                      <div className="flex items-center gap-3 mb-1">
                        <span className="text-xs font-black text-gray-500 uppercase tracking-widest">Pedido #{order.id.slice(0, 8)}</span>
                        <span className={`text-[10px] font-black uppercase tracking-widest px-2 py-0.5 rounded-full ${status.bg} ${status.color}`}>
                          {status.label}
                        </span>
                      </div>
                      <h3 className="text-lg font-bold">R$ {order.total_price.toFixed(2)}</h3>
                      <p className="text-xs text-gray-500 font-bold">
                        {format(new Date(order.created_at!), "d 'de' MMMM 'às' HH:mm", { locale: ptBR })}
                      </p>
                    </div>
                  </div>

                  <div className="flex flex-col md:flex-row items-stretch md:items-center gap-4">
                    <div className="flex flex-col md:items-end">
                      <span className="text-[10px] font-black text-gray-500 uppercase tracking-widest mb-1">Pagamento</span>
                      <span className="text-sm font-bold uppercase">{order.payment_method}</span>
                    </div>

                    <div className="h-px md:h-8 w-full md:w-px bg-white/5" />

                    <div className="flex flex-col md:items-end">
                      <span className="text-[10px] font-black text-gray-500 uppercase tracking-widest mb-1">Entrega em</span>
                      <p className="text-sm font-bold truncate max-w-[200px]">{order.location}</p>
                    </div>

                    <div className="hidden md:flex items-center justify-center w-10 h-10 bg-white/5 rounded-full group-hover:bg-primary/20 group-hover:text-primary transition-colors">
                      <ChevronRight size={20} />
                    </div>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
