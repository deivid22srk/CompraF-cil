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
    <div className="bg-background min-h-screen text-white">
      <Helmet>
        <title>{product.name} | CompraFácil</title>
        <meta name="description" content={product.description || `Confira ${product.name} no CompraFácil.`} />
        <meta property="og:title" content={product.name} />
        <meta property="og:description" content={product.description} />
        <meta property="og:image" content={product.image_url} />
        <meta property="og:type" content="product" />
      </Helmet>

      {/* Mobile-style Header */}
      <div className="sticky top-0 z-50 bg-background/80 backdrop-blur-md p-4 flex items-center justify-between max-w-4xl mx-auto w-full">
        <button
          onClick={() => navigate('/')}
          className="p-2 bg-card rounded-xl text-primary hover:bg-primary hover:text-black transition-all"
        >
          <ArrowLeft size={20} />
        </button>
        <h2 className="text-sm font-black uppercase tracking-widest truncate max-w-[200px]">Detalhes</h2>
        <button
          onClick={shareProduct}
          className="p-2 bg-card rounded-xl text-primary hover:bg-primary hover:text-black transition-all"
        >
          <Share2 size={20} />
        </button>
      </div>

      <div className="max-w-4xl mx-auto px-4 pb-20">
        <div className="grid lg:grid-cols-2 gap-8 items-start">
          <div className="space-y-4">
            <div className="aspect-square bg-card rounded-[2.5rem] overflow-hidden border border-white/5 shadow-2xl">
              <ImageWithLoading
                src={selectedImage || product.image_url}
                alt={product.name}
                className="w-full h-full object-cover transition-all duration-500"
                containerClassName="w-full h-full"
              />
            </div>
            {allImages.length > 1 && (
              <ImageGallery
                images={allImages}
                selectedImage={selectedImage}
                onSelect={setSelectedImage}
              />
            )}
          </div>

          <div className="flex flex-col gap-6">
            <div>
              <div className="flex items-center gap-2 mb-2">
                 <span className="text-[10px] font-black text-primary uppercase tracking-[0.2em]">{product.sold_by || 'CompraFácil'}</span>
              </div>
              <h1 className="text-2xl md:text-3xl font-black leading-tight mb-4 uppercase italic tracking-tighter">{product.name}</h1>

              <div className="flex items-center justify-between bg-card p-4 rounded-2xl border border-white/5">
                <span className="text-3xl font-black text-primary italic tracking-tighter">R$ {product.price.toFixed(2)}</span>
                {product.stock_quantity !== undefined && (
                  <span className={`text-[8px] font-black px-3 py-1 rounded-full uppercase tracking-widest ${product.stock_quantity === 0 ? 'bg-red-500/10 text-red-500' : 'bg-green-500/10 text-green-500'}`}>
                    {product.stock_quantity === 0 ? 'Esgotado' : `${product.stock_quantity} Em estoque`}
                  </span>
                )}
              </div>
            </div>

            {product.description && (
              <div className="space-y-2">
                <span className="text-[10px] font-black text-gray-500 uppercase tracking-widest">Descrição</span>
                <p className="text-sm text-gray-400 leading-relaxed font-medium bg-card/30 p-4 rounded-2xl border border-white/5">
                  {product.description}
                </p>
              </div>
            )}

            {product.variations && product.variations.length > 0 && (
              <div className="space-y-6">
                {product.variations.map((v, i) => (
                  <div key={i} className="space-y-3">
                    <span className="text-[10px] font-black text-gray-500 uppercase tracking-widest px-1">{v.name}</span>
                    <div className="flex flex-wrap gap-2">
                      {v.values.map((val, j) => (
                        <span
                          key={j}
                          className="px-5 py-2.5 rounded-xl text-xs font-black bg-card border border-white/5 text-gray-400"
                        >
                          {val}
                        </span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}

            <div className="mt-4 p-6 bg-primary/10 rounded-[2rem] border border-primary/20 space-y-4">
              <div className="flex items-center gap-3 text-primary">
                <Smartphone size={20} />
                <span className="font-black uppercase tracking-widest text-xs italic">Peça agora no aplicativo</span>
              </div>
              <p className="text-xs text-gray-400 font-medium leading-relaxed">
                Baixe o CompraFacil para adicionar ao carrinho, aplicar cupons de desconto e receber em casa com agilidade.
              </p>

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
          </div>
        </div>
      </div>
    </div>
  )
}
