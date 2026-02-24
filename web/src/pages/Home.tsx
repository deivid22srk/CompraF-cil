import { useEffect, useState } from 'react'
import { Search, SearchX } from 'lucide-react'
import { productService } from '../services/productService'
import type { Product } from '../types/database'
import ProductCard from '../components/ProductCard'
import LoadingSpinner from '../components/LoadingSpinner'

export default function Home() {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')

  useEffect(() => {
    async function fetchData() {
      try {
        const [productsData] = await Promise.all([
          productService.getProducts()
        ])

        setProducts(productsData)
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
    <div className="min-h-screen bg-background text-white">
      <div className="bg-gradient-to-b from-primary to-orange-400 p-8 md:p-12 rounded-b-[3rem] shadow-2xl mb-8">
        <div className="max-w-6xl mx-auto">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-8">
            <div className="space-y-2">
              <h1 className="text-4xl md:text-5xl font-black text-black tracking-tight italic uppercase">Promoções da Semana</h1>
              <p className="text-black/70 font-bold text-sm md:text-base uppercase tracking-[0.2em]">As melhores ofertas para você</p>
            </div>
            <div className="relative flex-1 max-w-lg">
              <div className="absolute inset-0 bg-black/10 blur-xl rounded-3xl"></div>
              <div className="relative">
                <Search className="absolute left-5 top-1/2 -translate-y-1/2 text-primary w-6 h-6" />
                <input
                  type="text"
                  placeholder="O que você procura hoje?"
                  className="w-full pl-14 pr-6 py-5 rounded-[1.5rem] bg-white text-black text-lg font-medium focus:outline-none shadow-2xl placeholder:text-gray-400"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <main className="max-w-6xl mx-auto p-6">
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
