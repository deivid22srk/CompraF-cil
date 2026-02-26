import { useEffect, useState } from 'react'
import { Search, SearchX, Download, Smartphone } from 'lucide-react'
import { productService } from '../services/productService'
import { configService } from '../services/configService'
import type { Product } from '../types/database'
import ProductCard from '../components/ProductCard'
import LoadingSpinner from '../components/LoadingSpinner'

export default function Home() {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [downloadUrl, setDownloadUrl] = useState('')

  useEffect(() => {
    async function fetchData() {
      try {
        const [productsData, config] = await Promise.all([
          productService.getProducts(),
          configService.getConfig()
        ])

        setProducts(productsData)
        if (config.download_url) setDownloadUrl(config.download_url)
      } catch (error) {
        console.error('Error fetching data:', error)
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [])

  const filteredProducts = products.filter(p =>
    p.name.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="bg-background text-white min-h-screen">
      <div className="bg-gradient-to-b from-primary to-orange-500 p-8 md:p-16 rounded-b-[3rem] shadow-2xl mb-12 text-center">
        <div className="max-w-4xl mx-auto space-y-8">
          <div className="space-y-4">
            <h1 className="text-4xl md:text-6xl font-black text-black tracking-tighter uppercase italic">CompraFacil</h1>
            <p className="text-black font-bold text-sm md:text-base uppercase tracking-[0.3em] opacity-80">Catálogo de Produtos Online</p>
          </div>

          <div className="bg-black/10 backdrop-blur-md rounded-[2rem] p-6 md:p-10 border border-black/5">
            <h2 className="text-xl md:text-2xl font-black text-black mb-4 uppercase">Faça seu pedido pelo App!</h2>
            <p className="text-black/70 font-medium mb-8 text-sm md:text-base">Para uma experiência completa com carrinho, pagamentos e rastreio em tempo real, baixe nosso aplicativo oficial.</p>

            {downloadUrl && (
              <a
                href={downloadUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-3 bg-black text-primary font-black px-10 py-5 rounded-2xl text-lg shadow-2xl hover:scale-105 active:scale-95 transition-all uppercase tracking-widest"
              >
                <Download size={24} /> Baixar App Agora
              </a>
            )}
          </div>

          <div className="relative max-w-2xl mx-auto">
            <Search className="absolute left-6 top-1/2 -translate-y-1/2 text-primary w-6 h-6" />
            <input
              type="text"
              placeholder="O que você procura hoje?"
              className="w-full pl-16 pr-8 py-5 rounded-[1.5rem] bg-white text-black text-lg font-bold focus:outline-none shadow-2xl placeholder:text-gray-400 border-none"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        </div>
      </div>

      <main className="max-w-6xl mx-auto px-6">
        <div className="bg-primary/5 border border-primary/20 rounded-[2rem] p-8 mb-16 flex flex-col md:flex-row items-center justify-center gap-6 text-center md:text-left">
          <div className="w-12 h-12 rounded-full bg-primary/20 flex items-center justify-center text-primary animate-pulse">
            <Smartphone size={24} />
          </div>
          <div>
            <p className="text-primary font-black text-lg uppercase tracking-widest">
              Atendimento Exclusivo
            </p>
            <p className="text-gray-400 font-bold text-sm uppercase tracking-tighter">
              Sítio Riacho dos Barreiros e locais próximos
            </p>
          </div>
        </div>

        {loading ? (
          <LoadingSpinner />
        ) : filteredProducts.length > 0 ? (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {filteredProducts.map(product => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-20 text-gray-500">
            <SearchX size={64} className="mb-4 opacity-20" />
            <h3 className="text-xl font-bold text-white mb-2">Nenhum produto encontrado</h3>
            <p>Tente buscar por outro termo.</p>
          </div>
        )}
      </main>
    </div>
  )
}
