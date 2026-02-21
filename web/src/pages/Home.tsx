import { useEffect, useState } from 'react'
import { Search, Download, SearchX } from 'lucide-react'
import { productService } from '../services/productService'
import { configService } from '../services/configService'
import type { Product } from '../types/database'
import ProductCard from '../components/ProductCard'
import LoadingSpinner from '../components/LoadingSpinner'

export default function Home() {
  const [products, setProducts] = useState<Product[]>([])
  const [downloadUrl, setDownloadUrl] = useState('')
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')

  useEffect(() => {
    async function fetchData() {
      try {
        const [productsData, url] = await Promise.all([
          productService.getProducts(),
          configService.getDownloadUrl()
        ])

        setProducts(productsData)
        if (url) setDownloadUrl(url)
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
    <div>
      <header className="bg-primary p-6 shadow-lg">
        <div className="max-w-6xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <h1 className="text-3xl font-black text-black">CompraFácil</h1>
            {downloadUrl && (
              <a
                href={downloadUrl}
                target="_blank"
                rel="noreferrer"
                className="bg-black text-primary px-4 py-2 rounded-full text-sm font-bold flex items-center gap-2 hover:scale-105 transition-transform"
              >
                <Download size={16} />
                Baixar App
              </a>
            )}
          </div>
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 w-5 h-5" />
            <input
              type="text"
              placeholder="Buscar produtos..."
              className="w-full pl-10 pr-4 py-3 rounded-2xl bg-white text-black focus:outline-none"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto p-6">
        <div className="bg-primary/10 border border-primary/20 rounded-xl p-4 mb-8 text-center">
          <p className="text-primary font-bold">
            Disponível apenas em Sítio Riacho dos Barreiros e locais próximos
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
