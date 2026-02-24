import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Share2, ShoppingCart, CheckCircle2, Plus, Minus, Loader2 } from 'lucide-react'
import { Helmet } from 'react-helmet-async'
import { productService } from '../services/productService'
import { authService } from '../services/authService'
import { cartService } from '../services/cartService'
import type { Product, ProductImage, CartItem } from '../types/database'
import LoadingSpinner from '../components/LoadingSpinner'
import ImageGallery from '../components/ImageGallery'
import ImageWithLoading from '../components/ImageWithLoading'

export default function ProductDetails() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [product, setProduct] = useState<Product | null>(null)
  const [images, setImages] = useState<ProductImage[]>([])
  const [selectedImage, setSelectedImage] = useState('')
  const [loading, setLoading] = useState(true)
  const [isAdding, setIsAdding] = useState(false)
  const [quantity, setQuantity] = useState(1)
  const [selectedVariations, setSelectedVariations] = useState<Record<string, string>>({})

  const isOutOfStock = product?.stock_quantity === 0;
  const maxStock = product?.stock_quantity ?? 99;

  useEffect(() => {
    if (id) {
      fetchData(id)
    }
  }, [id])

  async function fetchData(productId: string) {
    try {
      const [productData, imagesData] = await Promise.all([
        productService.getProductById(productId),
        productService.getProductImages(productId)
      ])

      if (productData) {
        setProduct(productData)
        setSelectedImage(productData.image_url)
      }
      if (imagesData) setImages(imagesData)
    } catch (error) {
      console.error('Error fetching product details:', error)
    } finally {
      setLoading(false)
    }
  }

  const shareProduct = () => {
    if (navigator.share && product) {
      navigator.share({
        title: product.name,
        text: `Confira este produto no CompraFácil: ${product.name}`,
        url: window.location.href,
      })
    }
  }

  if (loading) return <LoadingSpinner />

  if (!product) return (
    <div className="p-10 text-center">
      <h2 className="text-2xl font-bold mb-4">Produto não encontrado</h2>
      <button onClick={() => navigate('/')} className="text-primary underline">Voltar ao início</button>
    </div>
  )

  const allImages = [
    { id: 'main', image_url: product.image_url },
    ...images.filter(img => img.image_url !== product.image_url)
  ]

  const handleAddToCart = async () => {
    try {
      const user = await authService.getUser()
      if (!user) {
        navigate('/login')
        return
      }

      // Check variations
      const missing = product.variations?.find(v => !selectedVariations[v.name])
      if (missing) {
        alert(`Por favor, selecione: ${missing.name}`)
        return
      }

      setIsAdding(true)
      const cartItem: CartItem = {
        user_id: user.id,
        product_id: product.id,
        quantity,
        selected_variations: Object.keys(selectedVariations).length > 0 ? selectedVariations : null
      }

      await cartService.addToCart(cartItem)
      alert('Produto adicionado ao carrinho!')
    } catch (error: any) {
      alert('Erro ao adicionar: ' + error.message)
    } finally {
      setIsAdding(false)
    }
  }

  const handleBuyNow = async () => {
    const user = await authService.getUser()
    if (!user) {
      navigate('/login')
      return
    }

    // Check variations
    const missing = product.variations?.find(v => !selectedVariations[v.name])
    if (missing) {
      alert(`Por favor, selecione: ${missing.name}`)
      return
    }

    const varsJson = encodeURIComponent(JSON.stringify(selectedVariations))
    navigate(`/checkout?productId=${product.id}&quantity=${quantity}&variations=${varsJson}`)
  }

  return (
    <div className="max-w-4xl mx-auto p-6 lg:py-12">
      <Helmet>
        <title>{product.name} | CompraFácil</title>
        <meta name="description" content={product.description || `Confira ${product.name} no CompraFácil.`} />
        <meta property="og:title" content={product.name} />
        <meta property="og:description" content={product.description} />
        <meta property="og:image" content={product.image_url} />
        <meta property="og:type" content="product" />
      </Helmet>

      <button
        onClick={() => navigate('/')}
        className="flex items-center gap-2 text-gray-400 hover:text-white mb-6 transition-colors"
      >
        <ArrowLeft size={20} /> Voltar
      </button>

      <div className="grid md:grid-cols-2 gap-10 bg-card rounded-[40px] overflow-hidden shadow-2xl">
        <div className="flex flex-col">
          <div className="h-[400px] bg-surface">
            <ImageWithLoading
              src={selectedImage || product.image_url}
              alt={product.name}
              className="w-full h-full object-cover transition-all duration-300"
              containerClassName="w-full h-full"
            />
          </div>
          <ImageGallery
            images={allImages}
            selectedImage={selectedImage}
            onSelect={setSelectedImage}
          />
        </div>

        <div className="p-10 flex flex-col">
          <div className="flex justify-between items-start mb-4">
            <h1 className="text-3xl font-bold">{product.name}</h1>
            <button onClick={shareProduct} className="p-2 bg-surface rounded-full text-primary hover:bg-primary hover:text-black transition-all">
              <Share2 size={24} />
            </button>
          </div>

          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-2 text-primary">
              <span className="text-3xl font-black">R$ {product.price.toFixed(2)}</span>
            </div>
            {product.stock_quantity !== undefined && (
              <span className={`text-[10px] font-black px-3 py-1 rounded-full uppercase tracking-widest ${isOutOfStock ? 'bg-red-500/10 text-red-500 border border-red-500/20' : 'bg-green-500/10 text-green-500 border border-green-500/20'}`}>
                {isOutOfStock ? 'Esgotado' : `${product.stock_quantity} Disponíveis`}
              </span>
            )}
          </div>

          <p className="text-gray-400 leading-relaxed mb-6 flex-1">
            {product.description || 'Nenhuma descrição disponível para este produto.'}
          </p>

          {product.variations && product.variations.length > 0 && (
            <div className="space-y-6 mb-10">
              {product.variations.map((v, i) => (
                <div key={i}>
                  <span className="text-[10px] font-black text-gray-500 uppercase tracking-widest block mb-3 px-1">{v.name}</span>
                  <div className="flex flex-wrap gap-2">
                    {v.values.map((val, j) => (
                      <button
                        key={j}
                        onClick={() => setSelectedVariations(prev => ({ ...prev, [v.name]: val }))}
                        className={`px-6 py-3 rounded-2xl text-sm font-black transition-all border ${
                          selectedVariations[v.name] === val
                            ? 'bg-primary border-primary text-black'
                            : 'bg-surface border-white/5 text-gray-400 hover:border-primary/50'
                        }`}
                      >
                        {val}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Quantidade e Compra */}
          <div className="mt-auto space-y-6">
            <div className="flex items-center justify-between gap-6 p-4 bg-surface rounded-3xl border border-white/5">
              <span className="text-xs font-black text-gray-500 uppercase tracking-widest pl-2">Quantidade</span>
              <div className="flex items-center bg-card rounded-2xl border border-white/5">
                <button
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  className="p-3 text-gray-400 hover:text-white disabled:opacity-20"
                  disabled={isOutOfStock}
                >
                  <Minus size={20} />
                </button>
                <span className="w-10 text-center font-black text-lg">{isOutOfStock ? 0 : quantity}</span>
                <button
                  onClick={() => setQuantity(Math.min(maxStock, quantity + 1))}
                  className="p-3 text-primary disabled:opacity-20"
                  disabled={isOutOfStock || quantity >= maxStock}
                >
                  <Plus size={20} />
                </button>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <button
                onClick={handleAddToCart}
                disabled={isAdding || isOutOfStock}
                className="flex items-center justify-center gap-3 py-5 bg-card border border-white/10 rounded-[1.5rem] font-black uppercase text-sm hover:bg-white/5 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isAdding ? <Loader2 className="animate-spin" /> : <ShoppingCart size={20} />}
                {isOutOfStock ? 'Sem Estoque' : 'Colocar no Carrinho'}
              </button>
              <button
                onClick={handleBuyNow}
                disabled={isOutOfStock}
                className="flex items-center justify-center gap-3 py-5 bg-primary text-black rounded-[1.5rem] font-black uppercase text-sm hover:scale-[1.02] active:scale-95 transition-all shadow-xl shadow-primary/20 disabled:opacity-50 disabled:bg-gray-600 disabled:cursor-not-allowed"
              >
                <CheckCircle2 size={20} />
                {isOutOfStock ? 'Indisponível' : 'Comprar Agora'}
              </button>
            </div>

            <div className="flex items-center justify-between p-4 bg-surface rounded-2xl border border-white/5">
              <span className="text-sm text-gray-400 font-bold uppercase tracking-tighter">Vendido por</span>
              <span className="font-black text-secondary">{product.sold_by || 'CompraFácil'}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
