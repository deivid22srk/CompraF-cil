import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ShoppingBag, Trash2, Plus, Minus, ArrowRight } from 'lucide-react'
import { authService } from '../services/authService'
import { cartService } from '../services/cartService'
import type { CartItem } from '../types/database'
import LoadingSpinner from '../components/LoadingSpinner'

export default function Cart() {
  const navigate = useNavigate()
  const [items, setItems] = useState<CartItem[]>([])
  const [loading, setLoading] = useState(true)
  const [userId, setUserId] = useState<string | null>(null)

  useEffect(() => {
    fetchData()
  }, [])

  async function fetchData() {
    try {
      const user = await authService.getUser()
      if (!user) {
        setLoading(false)
        return
      }
      setUserId(user.id)
      const cartItems = await cartService.getCartItems(user.id)
      setItems(cartItems)
    } catch (error) {
      console.error('Error fetching cart:', error)
    } finally {
      setLoading(false)
    }
  }

  const updateQuantity = async (itemId: string, quantity: number) => {
    if (quantity < 1) return
    try {
      await cartService.updateQuantity(itemId, quantity)
      setItems(items.map(item => item.id === itemId ? { ...item, quantity } : item))
    } catch (error) {
      console.error('Error updating quantity:', error)
    }
  }

  const removeItem = async (itemId: string) => {
    try {
      await cartService.removeItem(itemId)
      setItems(items.filter(item => item.id !== itemId))
    } catch (error) {
      console.error('Error removing item:', error)
    }
  }

  if (loading) return <LoadingSpinner />

  if (!userId) {
    return (
      <div className="min-h-[calc(100vh-80px)] flex flex-col items-center justify-center p-6 text-center">
        <div className="bg-card p-12 rounded-[3rem] border border-white/5 shadow-2xl max-w-md w-full">
          <ShoppingBag size={80} className="mx-auto mb-6 text-primary opacity-20" />
          <h2 className="text-3xl font-black mb-4">Seu carrinho está vazio</h2>
          <p className="text-gray-400 mb-8 font-bold">Faça login para começar a adicionar produtos e fazer pedidos.</p>
          <Link to="/login" className="block w-full bg-primary text-black py-5 rounded-2xl font-black hover:scale-[1.02] transition-all">
            ENTRAR OU CADASTRAR
          </Link>
        </div>
      </div>
    )
  }

  const subtotal = items.reduce((acc, item) => acc + (item.product?.price || 0) * item.quantity, 0)

  return (
    <div className="max-w-4xl mx-auto p-6">
      <div className="flex items-center gap-4 mb-10">
        <div className="w-12 h-12 bg-primary rounded-2xl flex items-center justify-center text-black">
          <ShoppingBag size={24} />
        </div>
        <h1 className="text-4xl font-black">Meu Carrinho</h1>
      </div>

      {items.length === 0 ? (
        <div className="bg-card p-12 rounded-[3rem] border border-white/5 text-center">
          <p className="text-gray-400 font-bold mb-8 text-lg">Você ainda não adicionou nenhum produto.</p>
          <Link to="/" className="inline-block px-10 py-4 bg-primary text-black rounded-2xl font-black hover:scale-105 transition-all">
            VER PRODUTOS
          </Link>
        </div>
      ) : (
        <div className="grid lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-4">
            {items.map(item => (
              <div key={item.id} className="bg-card p-4 md:p-6 rounded-[2rem] border border-white/5 flex gap-4 md:gap-6 items-center">
                <img
                  src={item.product?.image_url}
                  alt={item.product?.name}
                  className="w-20 h-20 md:w-24 md:h-24 object-cover rounded-2xl"
                />
                <div className="flex-1 min-w-0">
                  <h3 className="font-black text-lg truncate">{item.product?.name}</h3>
                  {item.selected_variations && (
                    <div className="flex flex-wrap gap-1 mt-1">
                      {Object.entries(item.selected_variations).map(([k, v]) => (
                        <span key={k} className="text-[10px] font-black uppercase tracking-widest text-gray-500 bg-white/5 px-2 py-0.5 rounded-md">
                          {k}: {v}
                        </span>
                      ))}
                    </div>
                  )}
                  <p className="text-primary font-black mt-2 text-xl">
                    R$ {((item.product?.price || 0) * item.quantity).toFixed(2)}
                  </p>
                </div>

                <div className="flex flex-col items-end gap-3">
                  <div className="flex items-center bg-surface rounded-xl border border-white/5">
                    <button
                      onClick={() => updateQuantity(item.id!, item.quantity - 1)}
                      className="p-2 text-gray-400 hover:text-white"
                    >
                      <Minus size={16} />
                    </button>
                    <span className="w-8 text-center font-black text-sm">{item.quantity}</span>
                    <button
                      onClick={() => updateQuantity(item.id!, item.quantity + 1)}
                      className="p-2 text-primary"
                    >
                      <Plus size={16} />
                    </button>
                  </div>
                  <button
                    onClick={() => removeItem(item.id!)}
                    className="p-2 text-gray-600 hover:text-red-500 transition-colors"
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="lg:col-span-1">
            <div className="bg-card p-8 rounded-[3rem] border border-white/5 sticky top-28 shadow-2xl">
              <h2 className="text-2xl font-black mb-6">Resumo</h2>
              <div className="space-y-4 mb-8">
                <div className="flex justify-between text-gray-400 font-bold">
                  <span>Subtotal</span>
                  <span>R$ {subtotal.toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-gray-400 font-bold border-b border-white/5 pb-4">
                  <span>Entrega</span>
                  <span className="text-green-500">Grátis</span>
                </div>
                <div className="flex justify-between text-white text-2xl font-black pt-2">
                  <span>Total</span>
                  <span className="text-primary">R$ {subtotal.toFixed(2)}</span>
                </div>
              </div>

              <button
                onClick={() => navigate('/checkout')}
                className="w-full bg-primary text-black py-5 rounded-2xl font-black flex items-center justify-center gap-2 hover:scale-[1.02] active:scale-95 transition-all shadow-xl shadow-primary/20"
              >
                FINALIZAR PEDIDO
                <ArrowRight size={20} />
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
