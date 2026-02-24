import { useEffect, useState } from 'react'
import { Search, SearchX } from 'lucide-react'
import { productService } from '../services/productService'
import { configService } from '../services/configService'
import type { Product } from '../types/database'
import ProductCard from '../components/ProductCard'
import LoadingSpinner from '../components/LoadingSpinner'
import { Smartphone } from 'lucide-react'

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
    <div className="bg-background text-white">
      <div className="bg-gradient-to-b from-primary to-orange-400 p-6 md:p-10 rounded-b-[2.5rem] shadow-2xl mb-6">
        <div className="max-w-6xl mx-auto">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
            <div className="space-y-1">
              <h1 className="text-3xl md:text-4xl font-black text-black tracking-tight italic uppercase">Promoções</h1>
              <p className="text-black/70 font-bold text-[10px] md:text-xs uppercase tracking-[0.2em]">As melhores ofertas para você</p>
            </div>
            <div className="relative flex-1 max-w-md">
              <div className="relative">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-primary w-5 h-5" />
                <input
                  type="text"
                  placeholder="O que você procura hoje?"
                  className="w-full pl-12 pr-6 py-4 rounded-[1.2rem] bg-white text-black text-base font-medium focus:outline-none shadow-xl placeholder:text-gray-400"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <main className="max-w-6xl mx-auto px-4 py-4">
        {downloadUrl && (
          <div className="md:hidden bg-card border border-white/5 rounded-3xl p-6 mb-6 flex flex-col gap-4">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-primary/20 rounded-2xl flex items-center justify-center text-primary">
                <Smartphone size={24} />
              </div>
              <div>
                <h3 className="text-lg font-bold">CompraFacil no Android</h3>
                <p className="text-xs text-gray-500 font-bold uppercase tracking-widest">Experiência completa</p>
              </div>
            </div>
            <a
              href={downloadUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="bg-primary text-white font-black py-4 rounded-2xl text-center uppercase tracking-widest text-xs shadow-lg shadow-primary/20 hover:scale-[1.02] active:scale-[0.98] transition-all"
            >
              Baixar Aplicativo
            </a>
          </div>
        )}

        <div className="bg-primary/5 border border-primary/10 rounded-3xl p-6 mb-12 flex items-center justify-center gap-3">
          <div className="w-2 h-2 rounded-full bg-primary animate-pulse"></div>
          <p className="text-primary font-black text-sm uppercase tracking-widest">
            Entrega em Sítio Riacho dos Barreiros e região
          </p>
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
