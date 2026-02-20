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

interface ProductImage {
  id: string
  image_url: string
}

export default function ProductDetails() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [product, setProduct] = useState<Product | null>(null)
  const [images, setImages] = useState<ProductImage[]>([])
  const [selectedImage, setSelectedImage] = useState('')
  const [downloadUrl, setDownloadUrl] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (id) fetchData(id)
  }, [id])

  async function fetchData(productId: string) {
    // Fetch product
    const { data: productData } = await supabase
      .from('products')
      .select('*')
      .eq('id', productId)
      .single()

    if (productData) {
      setProduct(productData)
      setSelectedImage(productData.image_url)
    }

    // Fetch extra images
    const { data: imagesData } = await supabase
      .from('product_images')
      .select('*')
      .eq('product_id', productId)

    if (imagesData) {
      setImages(imagesData)
    }

    // Fetch download URL
    const { data: configData } = await supabase
      .from('app_config')
      .select('value')
      .eq('key', 'download_url')
      .single()

    if (configData) {
      setDownloadUrl(configData.value as string)
    }

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

  const allImages = [
    { id: 'main', image_url: product.image_url },
    ...images.filter(img => img.image_url !== product.image_url)
  ]

  return (
    <div className="max-w-4xl mx-auto p-6">
      <button
        onClick={() => navigate('/')}
        className="flex items-center gap-2 text-gray-400 hover:text-white mb-6 transition-colors"
      >
        <ArrowLeft size={20} /> Voltar
      </button>

      <div className="grid md:grid-cols-2 gap-10 bg-card rounded-[40px] overflow-hidden shadow-2xl">
        <div className="flex flex-col">
          <div className="h-[400px] bg-surface">
            <img
              src={selectedImage || product.image_url}
              alt={product.name}
              className="w-full h-full object-cover transition-all duration-300"
            />
          </div>
          {allImages.length > 1 && (
            <div className="p-4 flex gap-2 overflow-x-auto bg-surface/50 scrollbar-hide">
              {allImages.map((img) => (
                <button
                  key={img.id}
                  onClick={() => setSelectedImage(img.image_url)}
                  className={`relative flex-shrink-0 w-20 h-20 rounded-xl overflow-hidden border-2 transition-all ${
                    selectedImage === img.image_url ? 'border-primary' : 'border-transparent opacity-60 hover:opacity-100'
                  }`}
                >
                  <img src={img.image_url} className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
          )}
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
              {downloadUrl ? (
                <a
                  href={downloadUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="block w-full py-4 bg-primary text-black text-center font-black rounded-2xl hover:scale-[1.02] active:scale-95 transition-all"
                >
                  BAIXAR APP COMPRAFÁCIL
                </a>
              ) : (
                <button disabled className="w-full py-4 bg-gray-600 text-black font-black rounded-2xl opacity-50">
                  DOWNLOAD INDISPONÍVEL
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
