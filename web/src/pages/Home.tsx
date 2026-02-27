import { useEffect, useState } from 'react'
import { Search, SearchX, Download, Smartphone } from 'lucide-react'
import { productService } from '../services/productService'
import { configService } from '../services/configService'
import type { Product, Category } from '../types/database'
import ProductCard from '../components/ProductCard'
import LoadingSpinner from '../components/LoadingSpinner'

export default function Home() {
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [downloadUrl, setDownloadUrl] = useState('')

  useEffect(() => {
    async function fetchData() {
      try {
        const [productsData, categoriesData, config] = await Promise.all([
          productService.getProducts(selectedCategory),
          productService.getCategories(),
          configService.getConfig()
        ])

        setProducts(productsData)
        setCategories(categoriesData)
        if (config.download_url) setDownloadUrl(config.download_url)
      } catch (error) {
        console.error('Error fetching data:', error)
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [selectedCategory])

  const filteredProducts = products.filter(p =>
    p.name.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="bg-background text-white min-h-screen flex flex-col items-center">
      {/* Mini Header / App Banner - only on mobile */}
      <div className="w-full max-w-lg bg-primary p-3 text-center flex items-center justify-center gap-2 sticky top-0 z-50 shadow-lg md:hidden">
        <Smartphone size={16} className="text-black" />
        <span className="text-black text-[10px] font-black uppercase tracking-wider">Peça pelo App para mais recursos!</span>
        {downloadUrl && (
          <a href={downloadUrl} target="_blank" className="bg-black text-white px-3 py-1 rounded-full text-[10px] font-bold uppercase ml-2">Baixar</a>
        )}
      </div>

      <div className="w-full max-w-screen-xl flex flex-col md:flex-row md:items-start gap-8 p-4 md:p-8">
        {/* Main Content Area */}
        <div className="flex-1 w-full max-w-4xl mx-auto">
          <header className="mb-8 pt-4">
            <h1 className="text-3xl font-black italic tracking-tighter uppercase text-primary mb-1">CompraFacil</h1>
            <p className="text-xs font-bold text-gray-500 uppercase tracking-widest mb-6">Sua compra na palma da mão</p>

            <div className="relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-primary w-5 h-5" />
              <input
                type="text"
                placeholder="O que você procura hoje?"
                className="w-full pl-12 pr-4 py-3 rounded-2xl bg-card text-white text-sm font-medium focus:outline-none focus:ring-1 focus:ring-primary/50 transition-all"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          </header>

          <section className="mb-8">
            <div className="flex gap-2 overflow-x-auto pb-4 scrollbar-hide no-scrollbar">
              <button
                onClick={() => setSelectedCategory(null)}
                className={`px-6 py-2 rounded-full text-xs font-black uppercase tracking-widest transition-all whitespace-nowrap ${
                  selectedCategory === null ? 'bg-primary text-black' : 'bg-card text-gray-400'
                }`}
              >
                Tudo
              </button>
              {categories.map((cat) => (
                <button
                  key={cat.id}
                  onClick={() => setSelectedCategory(cat.id)}
                  className={`px-6 py-2 rounded-full text-xs font-black uppercase tracking-widest transition-all whitespace-nowrap ${
                    selectedCategory === cat.id ? 'bg-primary text-black' : 'bg-card text-gray-400'
                  }`}
                >
                  {cat.name}
                </button>
              ))}
            </div>
          </section>

          <main>
            {loading ? (
              <LoadingSpinner />
            ) : filteredProducts.length > 0 ? (
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-6">
                {filteredProducts.map(product => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center py-20 text-gray-500">
                <SearchX size={48} className="mb-4 opacity-20" />
                <h3 className="text-lg font-bold text-white mb-2">Nenhum produto encontrado</h3>
                <p className="text-sm">Tente buscar por outro termo ou categoria.</p>
              </div>
            )}
          </main>

          <footer className="mt-16 py-8 border-t border-white/5 text-center">
            <p className="text-[10px] text-gray-600 font-bold uppercase tracking-widest">
              Exclusivo para Sítio Riacho dos Barreiros e proximidades
            </p>
          </footer>
        </div>

        {/* Desktop Sidebar Promo */}
        <aside className="hidden lg:block w-80 sticky top-20 h-fit bg-card rounded-[2.5rem] p-8 border border-white/5">
           <h2 className="text-xl font-black text-primary mb-4 uppercase">EXPERIÊNCIA COMPLETA</h2>
           <p className="text-gray-400 text-sm font-medium mb-8 leading-relaxed">Baixe nosso app para ter acesso ao carrinho, cupons, rastreio em tempo real e pagamentos seguros.</p>
           {downloadUrl && (
              <a
                href={downloadUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center justify-center gap-3 bg-primary text-black font-black py-4 rounded-2xl text-sm shadow-xl hover:scale-105 active:scale-95 transition-all uppercase tracking-widest w-full"
              >
                <Download size={20} /> Baixar Agora
              </a>
            )}
            <div className="mt-8 pt-8 border-t border-white/5 flex flex-col gap-4">
               <div className="flex items-center gap-4 text-gray-500">
                  <div className="w-10 h-10 rounded-full bg-surface flex items-center justify-center text-primary">
                    <Smartphone size={20} />
                  </div>
                  <span className="text-xs font-bold uppercase tracking-tighter">Interface Mobile</span>
               </div>
            </div>
        </aside>
      </div>
    </div>
  )
}
