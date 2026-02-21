import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Share2, Smartphone } from 'lucide-react'
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
  const [downloadUrl, setDownloadUrl] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (id) {
      fetchData(id)
    }
  }, [id])

  async function fetchData(productId: string) {
    try {
      const [productData, imagesData, url] = await Promise.all([
        productService.getProductById(productId),
        productService.getProductImages(productId),
        configService.getDownloadUrl()
      ])

      if (productData) {
        setProduct(productData)
        setSelectedImage(productData.image_url)
      }
      if (imagesData) setImages(imagesData)
      if (url) setDownloadUrl(url)
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
    <div className="max-w-4xl mx-auto p-6">
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

          <div className="flex items-center gap-2 mb-6 text-primary">
            <span className="text-3xl font-black">R$ {product.price.toFixed(2)}</span>
          </div>

          <p className="text-gray-400 leading-relaxed mb-6 flex-1">
            {product.description || 'Nenhuma descrição disponível para este produto.'}
          </p>

          {product.variations && product.variations.length > 0 && (
            <div className="space-y-4 mb-8">
              {product.variations.map((v, i) => (
                <div key={i}>
                  <span className="text-sm font-bold text-gray-400 block mb-2">{v.name}</span>
                  <div className="flex flex-wrap gap-2">
                    {v.values.map((val, j) => (
                      <span
                        key={j}
                        className="px-4 py-2 bg-surface border border-white/10 rounded-xl text-sm font-medium text-white/80"
                      >
                        {val}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}

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
