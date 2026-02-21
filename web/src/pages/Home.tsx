import { useEffect, useState } from 'react'
import { supabase } from '../supabaseClient'
import { Link } from 'react-router-dom'
import { Search, Download } from 'lucide-react'

interface Product {
  id: string
  name: string
  price: number
  image_url: string
  sold_by?: string
}

export default function Home() {
  const [products, setProducts] = useState<Product[]>([])
  const [downloadUrl, setDownloadUrl] = useState('')
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')

  useEffect(() => {
    fetchData()
  }, [])

  async function fetchData() {
    // Fetch products
    const { data: productData } = await supabase
      .from('products')
      .select('*')
    if (productData) setProducts(productData)

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
          <div className="flex justify-center p-20">
            <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-primary"></div>
          </div>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {filteredProducts.map(product => (
              <Link
                key={product.id}
                to={`/product/${product.id}`}
                className="bg-card rounded-3xl overflow-hidden shadow-md hover:scale-105 transition-transform group"
              >
                <div className="relative h-48">
                  <img
                    src={product.image_url}
                    alt={product.name}
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute bottom-2 right-2 bg-secondary text-black px-3 py-1 rounded-lg text-sm font-bold shadow-sm">
                    R$ {product.price.toFixed(2)}
                  </div>
                </div>
                <div className="p-4">
                  <h3 className="font-bold text-lg truncate group-hover:text-primary transition-colors">
                    {product.name}
                  </h3>
                  <p className="text-xs text-gray-400 mt-1">
                    Por: {product.sold_by || 'CompraFácil'}
                  </p>
                </div>
              </Link>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
