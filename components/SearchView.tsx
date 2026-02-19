
import React, { useState, useEffect, useRef } from 'react';
import { Search, ArrowLeft, X, SlidersHorizontal, Star } from 'lucide-react';
import { supabase } from '../supabaseClient';
import { Product } from '../types';

interface SearchViewProps {
  onBack: () => void;
  onProductClick: (p: Product) => void;
  onAddToCart: (p: Product) => void;
}

const SearchView: React.FC<SearchViewProps> = ({ onBack, onProductClick, onAddToCart }) => {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  useEffect(() => {
    const searchProducts = async () => {
      if (!query.trim()) {
        setResults([]);
        return;
      }
      setLoading(true);
      const { data } = await supabase
        .from('products')
        .select('*')
        .ilike('name', `%${query}%`)
        .limit(10);

      if (data) setResults(data);
      setLoading(false);
    };

    const timer = setTimeout(searchProducts, 300);
    return () => clearTimeout(timer);
  }, [query]);

  return (
    <div className="min-h-screen bg-white animate-in slide-in-from-bottom duration-300 p-4">
      <div className="flex items-center gap-3 mb-6 pt-4">
        <button onClick={onBack} className="p-2 bg-slate-50 rounded-2xl">
          <ArrowLeft size={20} />
        </button>
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-300" size={18} />
          <input
            ref={inputRef}
            type="text"
            className="w-full bg-slate-50 pl-10 pr-10 py-3 rounded-2xl outline-none focus:ring-2 focus:ring-indigo-500 transition-all text-sm"
            placeholder="Pesquise por produtos..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          {query && (
            <button onClick={() => setQuery('')} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-300">
              <X size={16} />
            </button>
          )}
        </div>
        <button className="p-3 bg-slate-50 rounded-2xl">
          <SlidersHorizontal size={18} className="text-slate-400" />
        </button>
      </div>

      <div className="space-y-4">
        {loading ? (
          <div className="text-center py-10">
            <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
            <p className="text-xs text-slate-400">Procurando...</p>
          </div>
        ) : results.length > 0 ? (
          <div className="grid gap-4">
            {results.map(p => (
              <div key={p.id} className="bg-white p-3 rounded-2xl border border-slate-50 flex gap-4 cursor-pointer hover:bg-slate-50 transition-colors" onClick={() => onProductClick(p)}>
                <div className="w-20 h-20 bg-slate-100 rounded-xl overflow-hidden p-2 flex items-center justify-center">
                  <img src={p.image_url} alt={p.name} className="w-full h-full object-contain" />
                </div>
                <div className="flex-1 flex flex-col justify-center">
                  <h4 className="text-sm font-bold text-slate-800 line-clamp-1">{p.name}</h4>
                  <div className="flex items-center gap-1 my-1">
                    <Star size={10} className="text-yellow-400 fill-yellow-400" />
                    <span className="text-[10px] text-slate-400">{p.rating}</span>
                  </div>
                  <p className="text-indigo-600 font-black text-sm">R$ {p.price.toLocaleString('pt-BR')}</p>
                </div>
                <button
                  onClick={(e) => { e.stopPropagation(); onAddToCart(p); }}
                  className="w-8 h-8 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center my-auto"
                >
                  +
                </button>
              </div>
            ))}
          </div>
        ) : query ? (
          <div className="text-center py-20">
             <div className="w-16 h-16 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-4">
               <Search size={24} className="text-slate-200" />
             </div>
             <p className="text-sm font-bold text-slate-400">Nenhum resultado encontrado</p>
          </div>
        ) : (
          <div className="pt-4">
            <h5 className="text-xs font-black text-slate-400 uppercase tracking-widest mb-4">Pesquisas Recentes</h5>
            <div className="flex flex-wrap gap-2">
              {['Beats', 'Tênis', 'Watch', 'Fone'].map(tag => (
                <button key={tag} onClick={() => setQuery(tag)} className="px-4 py-2 bg-slate-50 rounded-xl text-xs font-bold text-slate-600 hover:bg-indigo-50 hover:text-indigo-600 transition-colors">
                  {tag}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default SearchView;
