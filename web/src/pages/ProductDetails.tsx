import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Share2, Smartphone, Download } from 'lucide-react'
import { Helmet } from 'react-helmet-async'
import { productService } from '../services/productService'
import { configService } from '../services/configService'
import type { Product, ProductImage } from '../types/database'
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
  const [downloadUrl, setDownloadUrl] = useState('')

  useEffect(() => {
    if (id) {
      fetchData(id)
    }
  }, [id])

  async function fetchData(productId: string) {
    try {
      const [productData, imagesData, config] = await Promise.all([
        productService.getProductById(productId),
        productService.getProductImages(productId),
        configService.getConfig()
      ])

      if (productData) {
        setProduct(productData)
        setSelectedImage(productData.image_url)
      }
      if (imagesData) setImages(imagesData)
      if (config.download_url) setDownloadUrl(config.download_url)
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
              <span className={`text-[10px] font-black px-3 py-1 rounded-full uppercase tracking-widest ${product.stock_quantity === 0 ? 'bg-red-500/10 text-red-500 border border-red-500/20' : 'bg-green-500/10 text-green-500 border border-green-500/20'}`}>
                {product.stock_quantity === 0 ? 'Esgotado' : `${product.stock_quantity} Disponíveis`}
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
                      <span
                        key={j}
                        className="px-6 py-3 rounded-2xl text-sm font-black bg-surface border border-white/5 text-gray-400"
                      >
                        {val}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="mt-auto space-y-6">
            <div className="bg-primary/5 border border-primary/20 rounded-3xl p-6 text-center space-y-4">
              <div className="flex items-center justify-center gap-2 text-primary">
                <Smartphone size={20} />
                <span className="font-black uppercase tracking-widest text-xs">Compre pelo Aplicativo</span>
              </div>
              <p className="text-sm text-gray-400 font-medium">Baixe nosso app para adicionar ao carrinho e fazer seu pedido com facilidade.</p>

              {downloadUrl && (
                <a
                  href={downloadUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-center gap-3 py-4 bg-primary text-black rounded-2xl font-black uppercase text-sm hover:scale-[1.02] active:scale-95 transition-all shadow-xl shadow-primary/20"
                >
                  <Download size={20} /> Baixar App Agora
                </a>
              )}
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
