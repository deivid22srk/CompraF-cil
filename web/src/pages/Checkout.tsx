import { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { MapPin, Phone, User, CreditCard, ArrowLeft, Loader2, CheckCircle2 } from 'lucide-react'
import { authService } from '../services/authService'
import { cartService } from '../services/cartService'
import { configService } from '../services/configService'
import { orderService } from '../services/orderService'
import { productService } from '../services/productService'
import type { Order, OrderItem } from '../types/database'
import LoadingSpinner from '../components/LoadingSpinner'

export default function Checkout() {
  const navigate = useNavigate()
  const location = useLocation()
  const [loading, setLoading] = useState(true)
  const [isPlacing, setIsPlacing] = useState(false)
  const [success, setSuccess] = useState(false)

  const [userId, setUserId] = useState('')
  const [customerName, setCustomerName] = useState('')
  const [whatsapp, setWhatsapp] = useState('')
  const [address, setAddress] = useState('')
  const [latitude, setLatitude] = useState<number | null>(null)
  const [longitude, setLongitude] = useState<number | null>(null)
  const [gettingLocation, setGettingLocation] = useState(false)
  const [paymentMethod, setPaymentMethod] = useState('dinheiro')
  const [deliveryFee, setDeliveryFee] = useState(0)

  const [items, setItems] = useState<any[]>([]) // CartItem or simplified direct purchase item

  // Direct purchase params from URL
  const searchParams = new URLSearchParams(location.search)
  const directProductId = searchParams.get('productId')
  const directQuantity = parseInt(searchParams.get('quantity') || '1')
  const directVariations = searchParams.get('variations')
    ? JSON.parse(decodeURIComponent(searchParams.get('variations')!))
    : null

  useEffect(() => {
    fetchData()
  }, [])

  async function fetchData() {
    try {
      const user = await authService.getUser()
      if (!user) {
        navigate('/login')
        return
      }
      setUserId(user.id)

      // Get profile for defaults
      try {
        const profile = await authService.getProfile(user.id)
        if (profile) {
          setCustomerName(profile.full_name || '')
          setWhatsapp(profile.whatsapp || '')
        }
      } catch (e) {}

      // Get delivery fee
      const fee = await configService.getDeliveryFee()
      setDeliveryFee(fee)

      if (directProductId) {
        const product = await productService.getProductById(directProductId)
        if (product) {
          setItems([{
            product,
            quantity: directQuantity,
            selected_variations: directVariations
          }])
        }
      } else {
        const cartItems = await cartService.getCartItems(user.id)
        if (cartItems.length === 0) {
          navigate('/cart')
          return
        }
        setItems(cartItems)
      }
    } catch (error) {
      console.error('Error fetching checkout data:', error)
    } finally {
      setLoading(false)
    }
  }

  const subtotal = items.reduce((acc, item) => acc + (item.product?.price || 0) * item.quantity, 0)
  const total = subtotal + deliveryFee

  const handleGetLocation = () => {
    if (!navigator.geolocation) {
      alert('Seu navegador não suporta geolocalização.')
      return
    }

    setGettingLocation(true)
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLatitude(position.coords.latitude)
        setLongitude(position.coords.longitude)
        setGettingLocation(false)
        alert('Localização capturada com sucesso!')
      },
      (error) => {
        console.error('Error getting location:', error)
        setGettingLocation(false)
        alert('Não foi possível obter sua localização. Por favor, preencha o endereço manualmente.')
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    )
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!customerName || !whatsapp || !address) {
      alert('Preencha todos os campos obrigatórios.')
      return
    }

    setIsPlacing(true)
    try {
      const order: Order = {
        user_id: userId,
        customer_name: customerName,
        whatsapp,
        location: address,
        total_price: total,
        latitude: latitude,
        longitude: longitude,
        payment_method: paymentMethod,
        status: 'pendente'
      }

      const orderItems: OrderItem[] = items.map(item => ({
        order_id: '', // Will be filled by service
        product_id: item.product_id || item.product.id,
        quantity: item.quantity,
        price_at_time: item.product.price,
        selected_variations: item.selected_variations
      }))

      await orderService.placeOrder(order, orderItems)

      if (!directProductId) {
        await cartService.clearCart(userId)
      }

      setSuccess(true)
    } catch (error: any) {
      alert('Erro ao realizar pedido: ' + error.message)
    } finally {
      setIsPlacing(false)
    }
  }

  if (loading) return <LoadingSpinner />

  if (success) {
    return (
      <div className="min-h-[calc(100vh-80px)] flex items-center justify-center p-6">
        <div className="bg-card p-12 rounded-[3.5rem] border border-white/5 shadow-2xl max-w-lg w-full text-center">
          <div className="w-24 h-24 bg-green-500/20 rounded-full flex items-center justify-center text-green-500 mx-auto mb-8 animate-bounce">
            <CheckCircle2 size={48} />
          </div>
          <h2 className="text-4xl font-black mb-4 uppercase">Pedido Realizado!</h2>
          <p className="text-gray-400 mb-10 font-bold text-lg leading-relaxed">
            Seu pedido foi enviado com sucesso. <br/>
            Aguarde o contato pelo WhatsApp para entrega.
          </p>
          <button
            onClick={() => navigate('/')}
            className="w-full bg-primary text-black py-5 rounded-[1.5rem] font-black hover:scale-105 transition-all shadow-xl shadow-primary/20"
          >
            VOLTAR PARA A LOJA
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-6xl mx-auto p-6 lg:p-10">
      <button
        onClick={() => navigate(-1)}
        className="flex items-center gap-2 text-gray-400 hover:text-white mb-8 font-bold transition-colors"
      >
        <ArrowLeft size={20} /> Voltar
      </button>

      <h1 className="text-5xl font-black mb-12">Finalizar Pedido</h1>

      <form onSubmit={handleSubmit} className="grid lg:grid-cols-12 gap-12">
        <div className="lg:col-span-7 space-y-10">
          {/* Identificação */}
          <section className="space-y-6">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-primary/20 rounded-xl flex items-center justify-center text-primary font-black">1</div>
              <h2 className="text-2xl font-black uppercase">Sua Identificação</h2>
            </div>
            <div className="grid md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <label className="text-[10px] font-black text-gray-500 uppercase tracking-widest px-1">Nome Completo</label>
                <div className="relative">
                  <User className="absolute left-5 top-1/2 -translate-y-1/2 text-gray-500 w-5 h-5" />
                  <input
                    required
                    value={customerName}
                    onChange={e => setCustomerName(e.target.value)}
                    className="w-full pl-14 pr-6 py-4 bg-card rounded-2xl border border-white/5 focus:border-primary outline-none transition-all font-bold"
                    placeholder="Como vamos te chamar?"
                  />
                </div>
              </div>
              <div className="space-y-2">
                <label className="text-[10px] font-black text-gray-500 uppercase tracking-widest px-1">WhatsApp</label>
                <div className="relative">
                  <Phone className="absolute left-5 top-1/2 -translate-y-1/2 text-gray-500 w-5 h-5" />
                  <input
                    required
                    value={whatsapp}
                    onChange={e => setWhatsapp(e.target.value)}
                    className="w-full pl-14 pr-6 py-4 bg-card rounded-2xl border border-white/5 focus:border-primary outline-none transition-all font-bold"
                    placeholder="(00) 00000-0000"
                  />
                </div>
              </div>
            </div>
          </section>

          {/* Entrega */}
          <section className="space-y-6">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-primary/20 rounded-xl flex items-center justify-center text-primary font-black">2</div>
              <h2 className="text-2xl font-black uppercase">Onde Entregar?</h2>
            </div>
            <div className="space-y-2">
              <div className="flex justify-between items-center px-1">
                <label className="text-[10px] font-black text-gray-500 uppercase tracking-widest">Endereço de Entrega</label>
                <button
                  type="button"
                  onClick={handleGetLocation}
                  disabled={gettingLocation}
                  className={`text-[10px] font-black uppercase tracking-widest flex items-center gap-1 transition-colors ${latitude ? 'text-green-500' : 'text-primary hover:text-orange-400'}`}
                >
                  {gettingLocation ? <Loader2 size={12} className="animate-spin" /> : <MapPin size={12} />}
                  {latitude ? 'Localização Capturada' : 'Obter Localização Exata'}
                </button>
              </div>
              <div className="relative">
                <MapPin className="absolute left-5 top-4 text-gray-500 w-5 h-5" />
                <textarea
                  required
                  rows={3}
                  value={address}
                  onChange={e => setAddress(e.target.value)}
                  className="w-full pl-14 pr-6 py-4 bg-card rounded-2xl border border-white/5 focus:border-primary outline-none transition-all font-bold resize-none"
                  placeholder="Rua, número, bairro e pontos de referência..."
                />
              </div>
              {latitude && (
                <p className="text-[10px] text-green-500/70 font-bold px-1 italic">
                  * Coordenadas GPS anexadas ao pedido para facilitar a entrega.
                </p>
              )}
            </div>
          </section>

          {/* Pagamento */}
          <section className="space-y-6">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-primary/20 rounded-xl flex items-center justify-center text-primary font-black">3</div>
              <h2 className="text-2xl font-black uppercase">Pagamento</h2>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <button
                type="button"
                onClick={() => setPaymentMethod('dinheiro')}
                className={`flex flex-col items-center gap-3 p-6 rounded-[2rem] border transition-all ${
                  paymentMethod === 'dinheiro' ? 'bg-primary/10 border-primary text-primary' : 'bg-card border-white/5 text-gray-400'
                }`}
              >
                <div className={`p-4 rounded-2xl ${paymentMethod === 'dinheiro' ? 'bg-primary text-black' : 'bg-surface text-gray-500'}`}>
                  <CreditCard size={24} />
                </div>
                <span className="font-black text-sm uppercase tracking-widest">Dinheiro</span>
              </button>
              <button
                type="button"
                onClick={() => setPaymentMethod('pix')}
                className={`flex flex-col items-center gap-3 p-6 rounded-[2rem] border transition-all ${
                  paymentMethod === 'pix' ? 'bg-primary/10 border-primary text-primary' : 'bg-card border-white/5 text-gray-400'
                }`}
              >
                <div className={`p-4 rounded-2xl ${paymentMethod === 'pix' ? 'bg-primary text-black' : 'bg-surface text-gray-500'}`}>
                  <span className="font-black">PIX</span>
                </div>
                <span className="font-black text-sm uppercase tracking-widest">Pix</span>
              </button>
            </div>
            <p className="text-center text-xs font-bold text-gray-500 bg-white/5 py-3 rounded-xl border border-white/5">
              O pagamento é feito diretamente ao entregador no ato da entrega.
            </p>
          </section>
        </div>

        <div className="lg:col-span-5">
          <div className="bg-card p-10 rounded-[3.5rem] border border-white/5 sticky top-28 shadow-2xl overflow-hidden">
            <div className="absolute top-0 left-0 w-full h-2 bg-primary"></div>
            <h2 className="text-3xl font-black mb-8 uppercase italic">Resumo Final</h2>

            <div className="space-y-6 mb-10 max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
              {items.map((item, idx) => (
                <div key={idx} className="flex justify-between gap-4 border-b border-white/5 pb-4 last:border-0 last:pb-0">
                  <div className="min-w-0">
                    <p className="font-black truncate uppercase text-sm">{item.product.name}</p>
                    <p className="text-xs text-gray-500 font-bold tracking-widest uppercase">{item.quantity} UN x R$ {item.product.price.toFixed(2)}</p>
                  </div>
                  <span className="font-black text-white whitespace-nowrap">R$ {(item.quantity * item.product.price).toFixed(2)}</span>
                </div>
              ))}
            </div>

            <div className="space-y-4 pt-6 border-t-2 border-dashed border-white/10">
              <div className="flex justify-between text-gray-400 font-black text-xs uppercase tracking-widest">
                <span>Subtotal</span>
                <span>R$ {subtotal.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-gray-400 font-black text-xs uppercase tracking-widest">
                <span>Taxa de Entrega</span>
                <span className={deliveryFee === 0 ? 'text-green-500' : ''}>
                  {deliveryFee === 0 ? 'GRÁTIS' : `R$ ${deliveryFee.toFixed(2)}`}
                </span>
              </div>
              <div className="flex justify-between text-white text-3xl font-black pt-4">
                <span className="italic uppercase">Total</span>
                <span className="text-primary">R$ {total.toFixed(2)}</span>
              </div>
            </div>

            <button
              type="submit"
              disabled={isPlacing}
              className="w-full mt-10 bg-primary text-black py-6 rounded-[1.5rem] font-black flex items-center justify-center gap-3 hover:scale-[1.02] active:scale-95 transition-all shadow-xl shadow-primary/30 disabled:opacity-50 disabled:scale-100"
            >
              {isPlacing ? (
                <Loader2 className="animate-spin" />
              ) : (
                <>
                  CONFIRMAR E PEDIR
                  <CheckCircle2 size={24} />
                </>
              )}
            </button>
          </div>
        </div>
      </form>
    </div>
  )
}
