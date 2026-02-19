
import React, { useState, useEffect } from 'react';
import { Search, Zap, Star } from 'lucide-react';
import { supabase } from '../supabaseClient';
import { Product } from '../types';

interface ShopHomeProps {
  onProductClick: (p: Product) => void;
  onAddToCart: (p: Product) => void;
  onOpenSearch: () => void;
}

const ShopHome: React.FC<ShopHomeProps> = ({ onProductClick, onAddToCart, onOpenSearch }) => {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchProducts = async () => {
      const { data, error } = await supabase
        .from('products')
        .select('*')
        .order('created_at', { ascending: false });

      if (!error && data) setProducts(data);
      setLoading(false);
    };
    fetchProducts();
  }, []);

  const categories = [
    { icon: '👟', name: 'Sneakers' },
    { icon: '⌚', name: 'Watches' },
    { icon: '🎮', name: 'Electronics' },
    { icon: '👕', name: 'Apparel' },
  ];

  return (
    <div className="p-4 pt-8 animate-in fade-in duration-500">
      {/* Header & Search Trigger */}
      <div className="relative mb-6 cursor-pointer" onClick={onOpenSearch}>
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={20} />
        <div className="w-full pl-12 pr-4 py-3 bg-slate-100 rounded-2xl text-slate-400 text-sm">
          O que você está procurando?
        </div>
      </div>

      {/* Hero Banner REMOVED per user request */}

      {/* Categories */}
      <div className="flex justify-between mb-8 overflow-x-auto no-scrollbar gap-4">
        {categories.map((cat, i) => (
          <div key={i} className="flex flex-col items-center min-w-[70px] cursor-pointer">
            <div className="w-14 h-14 rounded-full bg-white shadow-md flex items-center justify-center text-2xl mb-2 hover:scale-110 transition-transform">
              {cat.icon}
            </div>
            <span className="text-xs font-medium text-slate-600">{cat.name}</span>
          </div>
        ))}
      </div>

      {/* Featured Header */}
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-xl font-bold flex items-center gap-2">
          Destaques <Zap className="text-yellow-400 fill-yellow-400" size={18} />
        </h3>
        <button className="text-indigo-600 font-bold text-sm">Ver todos</button>
      </div>

      {/* Product Grid */}
      {loading ? (
        <div className="grid grid-cols-2 gap-4">
           {[1,2,3,4].map(n => (
             <div key={n} className="h-64 bg-slate-100 rounded-2xl animate-pulse"></div>
           ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-4">
          {products.map(product => (
            <div
              key={product.id}
              className="bg-white rounded-3xl p-3 shadow-sm border border-slate-50 relative group cursor-pointer"
              onClick={() => onProductClick(product)}
            >
              {product.original_price && (
                <div className="absolute top-3 left-3 bg-cyan-100 text-cyan-600 text-[10px] font-bold px-2 py-1 rounded-lg z-10">
                  {Math.round((1 - product.price/product.original_price) * 100)}% OFF
                </div>
              )}
              <div className="bg-slate-50 rounded-2xl p-2 mb-3 h-32 flex items-center justify-center">
                <img src={product.image_url} alt={product.name} className="w-full h-full object-contain mix-blend-multiply group-hover:scale-110 transition-transform" />
              </div>
              <h4 className="text-xs font-bold text-slate-800 line-clamp-1 mb-1">{product.name}</h4>
              <div className="flex items-center gap-1 mb-2">
                <Star className="text-yellow-400 fill-yellow-400" size={12} />
                <span className="text-[10px] font-bold text-slate-400">{product.rating} ({product.rating_count})</span>
              </div>
              <div className="flex justify-between items-end">
                <div>
                  <p className="text-indigo-600 font-black text-sm">R$ {product.price.toLocaleString('pt-BR')}</p>
                  {product.original_price && <p className="text-[10px] text-slate-300 line-through">R$ {product.original_price}</p>}
                </div>
                <button
                  onClick={(e) => { e.stopPropagation(); onAddToCart(product); }}
                  className="w-8 h-8 rounded-xl bg-slate-50 text-slate-400 flex items-center justify-center hover:bg-indigo-600 hover:text-white transition-colors"
                >
                  +
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default ShopHome;
