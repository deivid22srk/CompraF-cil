
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
    { name: 'Frutas' },
    { name: 'Vegetais' },
    { name: 'Laticínios' },
    { name: 'Padaria' },
  ];

  return (
    <div className="p-6 animate-in fade-in duration-500">
      {/* Location Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <div className="text-[#FDCB58]">
            <Star size={24} fill="#FDCB58" />
          </div>
          <div>
            <p className="text-[10px] text-gray-400">Entregar em</p>
            <p className="text-sm font-bold">Sua Localização</p>
          </div>
        </div>
        <div className="w-10 h-10 rounded-full bg-gray-800 flex items-center justify-center border border-gray-700">
           <User size={20} className="text-gray-400" />
        </div>
      </div>

      {/* Search Bar */}
      <div className="relative mb-8 cursor-pointer" onClick={onOpenSearch}>
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500" size={20} />
        <div className="w-full pl-12 pr-4 py-4 bg-white rounded-2xl text-gray-400 text-sm font-medium">
          Pesquisar itens...
        </div>
      </div>

      {/* Categories */}
      <div className="mb-10">
        <h3 className="text-lg font-bold mb-4">Explorar Categorias</h3>
        <div className="flex justify-between overflow-x-auto no-scrollbar gap-4">
          {categories.map((cat, i) => (
            <div key={i} className="flex flex-col items-center min-w-[75px] cursor-pointer">
              <div className="w-16 h-16 rounded-full bg-[#2A2A2A] flex items-center justify-center mb-2 hover:scale-105 transition-transform border border-gray-800 shadow-lg">
                <LayoutGrid size={24} className="text-[#FDCB58]" fill="#FDCB58" />
              </div>
              <span className="text-xs font-bold text-gray-300">{cat.name}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Suggested Section */}
      <div className="mb-6">
        <h3 className="text-lg font-bold mb-4">Sugerido Para Você</h3>

        {loading ? (
          <div className="grid grid-cols-2 gap-4">
            {[1,2,3,4].map(n => (
              <div key={n} className="h-64 bg-gray-800 rounded-3xl animate-pulse"></div>
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-4">
            {products.map(product => (
              <div
                key={product.id}
                className="bg-white rounded-[32px] p-4 shadow-xl relative group cursor-pointer overflow-hidden"
                onClick={() => onProductClick(product)}
              >
                <div className="aspect-square bg-gray-50 rounded-2xl mb-4 flex items-center justify-center overflow-hidden">
                  <img
                    src={product.image_url}
                    alt={product.name}
                    className="w-full h-full object-contain group-hover:scale-110 transition-transform duration-500"
                  />
                </div>

                <div className="space-y-1">
                  <h4 className="text-xs font-bold text-gray-400 uppercase tracking-tight line-clamp-1">{product.name}</h4>
                  <p className="text-lg font-black text-[#E67E22]">R$ {product.price.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</p>

                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-1">
                      <Star className="text-[#FDCB58] fill-[#FDCB58]" size={12} />
                      <span className="text-[10px] font-black text-gray-400">{product.rating}</span>
                    </div>
                    <button
                      onClick={(e) => { e.stopPropagation(); onAddToCart(product); }}
                      className="w-8 h-8 rounded-full bg-[#FDCB58] text-white flex items-center justify-center shadow-md active:scale-90 transition-transform"
                    >
                      <Plus size={16} strokeWidth={4} />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default ShopHome;
