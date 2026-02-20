import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { supabase } from '../supabaseClient'
import { ArrowLeft, Share2, Smartphone } from 'lucide-react'

interface Product {
  id: string
  name: string
  price: number
  description: string
  image_url: string
  sold_by?: string
  stock_quantity?: number
}

export default function ProductDetails() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [product, setProduct] = useState<Product | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (id) fetchProduct(id)
  }, [id])

  async function fetchProduct(productId: string) {
    const { data } = await supabase
      .from('products')
      .select('*')
      .eq('id', productId)
      .single()
    if (data) setProduct(data)
    setLoading(false)
  }

  const shareProduct = () => {
    if (navigator.share) {
      navigator.share({
        title: product?.name,
        text: `Confira este produto no CompraFácil: ${product?.name}`,
        url: window.location.href,
      })
    }
  }

  if (loading) return (
    <div className="flex justify-center p-20">
      <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-primary"></div>
    </div>
  )

  if (!product) return (
    <div className="p-10 text-center">
      <h2 className="text-2xl font-bold mb-4">Produto não encontrado</h2>
      <button onClick={() => navigate('/')} className="text-primary underline">Voltar ao início</button>
    </div>
  )

  return (
    <div className="max-w-4xl mx-auto p-6">
      <button
        onClick={() => navigate('/')}
        className="flex items-center gap-2 text-gray-400 hover:text-white mb-6 transition-colors"
      >
        <ArrowLeft size={20} /> Voltar
      </button>

      <div className="grid md:grid-cols-2 gap-10 bg-card rounded-[40px] overflow-hidden shadow-2xl">
        <div className="h-[400px] md:h-auto">
          <img src={product.image_url} alt={product.name} className="w-full h-full object-cover" />
        </div>
        <div className="p-10 flex flex-col">
          <div className="flex justify-between items-start mb-4">
            <h1 className="text-3xl font-bold">{product.name}</h1>
            <button onClick={shareProduct} className="p-2 bg-surface rounded-full text-primary hover:bg-primary hover:text-black transition-all">
              <Share2 size={24} />
            </button>
          </div>

          <div className="flex items-center gap-2 mb-6 text-primary">
            <span className="text-3xl font-black">R$ {product.price.toFixed(2)}</span>
          </div>

          <p className="text-gray-400 leading-relaxed mb-8 flex-1">
            {product.description || 'Nenhuma descrição disponível para este produto.'}
          </p>

          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-surface rounded-2xl border border-white/5">
              <span className="text-sm text-gray-400">Vendido por</span>
              <span className="font-bold text-secondary">{product.sold_by || 'CompraFácil'}</span>
            </div>

            <div className="bg-primary/10 border border-primary/20 p-6 rounded-3xl">
              <h4 className="font-bold text-primary mb-2 flex items-center gap-2">
                <Smartphone size={18} /> Baixe o App
              </h4>
              <p className="text-sm text-white/70 mb-4">
                Para comprar este produto e aproveitar ofertas exclusivas, baixe nosso aplicativo Android.
              </p>
              <button className="w-full py-4 bg-primary text-black font-black rounded-2xl hover:scale-[1.02] active:scale-95 transition-all">
                BAIXAR APP COMPRAFÁCIL
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
