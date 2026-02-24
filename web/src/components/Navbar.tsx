import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ShoppingCart, User, LogOut, Download, Menu, X } from 'lucide-react'
import { authService } from '../services/authService'
import { cartService } from '../services/cartService'
import { configService } from '../services/configService'

export default function Navbar() {
  const navigate = useNavigate()
  const [user, setUser] = useState<any>(null)
  const [cartCount, setCartCount] = useState(0)
  const [downloadUrl, setDownloadUrl] = useState('')
  const [isMenuOpen, setIsMenuOpen] = useState(false)

  useEffect(() => {
    // Initial user fetch
    authService.getUser().then(u => {
      setUser(u)
      if (u) fetchCartCount(u.id)
    })

    // Config fetch
    configService.getDownloadUrl().then(url => {
      if (url) setDownloadUrl(url)
    })

    // Listen for auth changes
    const { data: { subscription } } = authService.onAuthStateChange((_event, session) => {
      const u = session?.user || null
      setUser(u)
      if (u) fetchCartCount(u.id)
      else setCartCount(0)
    })

    return () => subscription.unsubscribe()
  }, [])

  async function fetchCartCount(userId: string) {
    try {
      const items = await cartService.getCartItems(userId)
      setCartCount(items.reduce((acc, item) => acc + item.quantity, 0))
    } catch (error) {
      console.error('Error fetching cart count:', error)
    }
  }

  const handleLogout = async () => {
    await authService.signOut()
    navigate('/')
  }

  return (
    <nav className="bg-background border-b border-white/5 sticky top-0 z-50">
      <div className="max-w-6xl mx-auto px-6 h-20 flex items-center justify-between">
        <Link to="/" className="text-2xl font-black text-primary">CompraFácil</Link>

        {/* Desktop Nav */}
        <div className="hidden md:flex items-center gap-8">
          {downloadUrl && (
            <a
              href={downloadUrl}
              target="_blank"
              rel="noreferrer"
              className="text-white/70 hover:text-white transition-colors flex items-center gap-2 font-bold text-sm"
            >
              <Download size={18} /> Baixar App
            </a>
          )}

          {user && (
            <Link to="/orders" className="text-white/70 hover:text-white transition-colors flex items-center gap-2 font-bold text-sm">
              Meus Pedidos
            </Link>
          )}

          <Link to="/cart" className="relative p-2 text-white/70 hover:text-primary transition-colors">
            <ShoppingCart size={24} />
            {cartCount > 0 && (
              <span className="absolute -top-1 -right-1 bg-primary text-black text-[10px] font-black w-5 h-5 flex items-center justify-center rounded-full">
                {cartCount}
              </span>
            )}
          </Link>

          {user ? (
            <div className="flex items-center gap-4">
              <Link to="/orders" className="flex items-center gap-2 text-sm font-bold text-white/90">
                <div className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center text-primary">
                  <User size={18} />
                </div>
                {user.email?.split('@')[0]}
              </Link>
              <button
                onClick={handleLogout}
                className="p-2 text-gray-500 hover:text-red-500 transition-colors"
                title="Sair"
              >
                <LogOut size={20} />
              </button>
            </div>
          ) : (
            <Link
              to="/login"
              className="bg-white text-black px-6 py-2.5 rounded-2xl text-sm font-black hover:bg-primary transition-colors"
            >
              ENTRAR
            </Link>
          )}
        </div>

        {/* Mobile Menu Toggle */}
        <button className="md:hidden text-white" onClick={() => setIsMenuOpen(!isMenuOpen)}>
          {isMenuOpen ? <X size={28} /> : <Menu size={28} />}
        </button>
      </div>

      {/* Mobile Nav */}
      {isMenuOpen && (
        <div className="md:hidden bg-card border-b border-white/5 p-6 space-y-6 animate-in slide-in-from-top duration-300">
          <Link to="/cart" className="flex items-center justify-between text-lg font-bold" onClick={() => setIsMenuOpen(false)}>
            <div className="flex items-center gap-3">
              <ShoppingCart className="text-primary" /> Carrinho
            </div>
            {cartCount > 0 && <span className="bg-primary text-black px-2 rounded-full text-xs font-black">{cartCount}</span>}
          </Link>

          {downloadUrl && (
            <a
              href={downloadUrl}
              target="_blank"
              rel="noreferrer"
              className="flex items-center gap-3 text-lg font-bold text-secondary"
              onClick={() => setIsMenuOpen(false)}
            >
              <Download /> Baixar Aplicativo
            </a>
          )}

          {user ? (
            <>
              <Link to="/orders" className="flex items-center gap-3 text-lg font-bold" onClick={() => setIsMenuOpen(false)}>
                <User className="text-primary" /> Meus Pedidos
              </Link>
              <button onClick={handleLogout} className="flex items-center gap-3 text-red-500 font-bold text-lg">
                <LogOut /> Sair
              </button>
            </>
          ) : (
            <Link
              to="/login"
              className="block w-full bg-primary text-black py-4 rounded-2xl text-center font-black"
              onClick={() => setIsMenuOpen(false)}
            >
              ENTRAR / REGISTRAR
            </Link>
          )}
        </div>
      )}
    </nav>
  )
}
