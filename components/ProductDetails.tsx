
import React from 'react';
import { ArrowLeft, Star, Heart } from 'lucide-react';
import { Product } from '../types';

interface ProductDetailsProps {
  product: Product;
  onBack: () => void;
  onAddToCart: (p: Product) => void;
}

const ProductDetails: React.FC<ProductDetailsProps> = ({ product, onBack, onAddToCart }) => {
  return (
    <div className="animate-in slide-in-from-right duration-500 min-h-screen bg-white">
      {/* Top Bar */}
      <div className="p-4 flex justify-between items-center">
        <button onClick={onBack} className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center">
          <ArrowLeft size={20} />
        </button>
        <h2 className="text-lg font-bold">Details</h2>
        <button className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center text-red-400">
          <Heart size={20} />
        </button>
      </div>

      {/* Main Image */}
      <div className="relative p-8 mb-4">
        <div className="absolute top-1/2 left-4 flex flex-col gap-4">
           {['#FF5F5F', '#5F5FFF', '#5FFF5F'].map(color => (
             <div key={color} className="w-6 h-6 rounded-full border-2 border-white shadow-sm" style={{ backgroundColor: color }}></div>
           ))}
        </div>
        <img src={product.image_url} alt={product.name} className="w-full h-64 object-contain" />
      </div>

      {/* Content Card */}
      <div className="p-6 pt-10 rounded-t-[40px] bg-white border-t border-slate-100 shadow-[0_-20px_50px_rgba(0,0,0,0.05)]">
        <div className="flex justify-between items-start mb-2">
          <h1 className="text-2xl font-black text-slate-800 flex-1">{product.name}</h1>
          <div className="flex items-center gap-1 bg-yellow-50 px-2 py-1 rounded-lg">
            <Star className="text-yellow-400 fill-yellow-400" size={16} />
            <span className="text-sm font-bold text-slate-800">{product.rating} <span className="text-slate-400 font-normal">({product.rating_count})</span></span>
          </div>
        </div>

        <div className="flex items-baseline gap-2 mb-6">
           <span className="text-2xl font-black text-indigo-600">R$ {product.price.toLocaleString('pt-BR')}</span>
           {product.original_price && <span className="text-slate-300 line-through">R$ {product.original_price}</span>}
        </div>

        <div className="bg-slate-50 p-4 rounded-2xl mb-6">
          <p className="text-xs font-bold text-slate-800 mb-1">Special request</p>
          <p className="text-[10px] text-slate-400">Special requests are welcome, but can't always be accommodated.</p>
        </div>

        <div className="mb-24">
          <h3 className="text-lg font-bold mb-2">Description</h3>
          <p className="text-sm text-slate-500 leading-relaxed">
            {product.description || "Este produto premium oferece qualidade superior e design moderno, ideal para quem busca o melhor custo-benefício do mercado."}
          </p>
          <div className="mt-4 flex gap-8">
            <div>
              <p className="text-xs text-slate-400">Brand</p>
              <p className="text-sm font-bold">{product.category}</p>
            </div>
            <div>
              <p className="text-xs text-slate-400">Model Name</p>
              <p className="text-sm font-bold">{product.name.split(' ')[0]}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Action */}
      <div className="fixed bottom-0 left-0 right-0 max-w-md mx-auto p-4 bg-white/80 backdrop-blur-md">
        <button
          onClick={() => onAddToCart(product)}
          className="w-full bg-indigo-600 text-white font-black py-4 rounded-2xl shadow-xl shadow-indigo-200 uppercase tracking-widest"
        >
          Add to Cart
        </button>
      </div>
    </div>
  );
};

export default ProductDetails;
