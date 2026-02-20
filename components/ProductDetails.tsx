
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
    <div className="animate-in slide-in-from-right duration-500 min-h-screen bg-[#F8F9FA] text-gray-900">
      {/* Fixed Top Bar */}
      <div className="fixed top-0 left-0 right-0 max-w-md mx-auto p-6 flex justify-between items-center z-[100]">
        <button onClick={onBack} className="w-12 h-12 rounded-full bg-white shadow-sm flex items-center justify-center border border-gray-100">
          <ArrowLeft size={24} />
        </button>
        <button className="w-12 h-12 rounded-full bg-white shadow-sm flex items-center justify-center text-gray-400 border border-gray-100">
          <Heart size={24} />
        </button>
      </div>

      {/* Hero Image */}
      <div className="w-full aspect-square flex items-center justify-center p-12 bg-white rounded-b-[60px] shadow-sm">
        <img src={product.image_url} alt={product.name} className="w-full h-full object-contain" />
      </div>

      {/* Content Section */}
      <div className="px-8 py-10 space-y-8">
        <div>
          <h1 className="text-3xl font-black text-gray-900 leading-tight mb-2">{product.name}</h1>
          <div className="flex items-center gap-4">
             <div className="flex items-center gap-1 bg-[#FDCB58]/10 px-3 py-1 rounded-full">
               <Star className="text-[#FDCB58] fill-[#FDCB58]" size={14} />
               <span className="text-xs font-black text-[#FDCB58]">{product.rating}</span>
             </div>
             <span className="text-xs font-bold text-gray-300 uppercase tracking-widest">{product.category}</span>
          </div>
        </div>

        <div>
          <p className="text-4xl font-black text-[#E67E22]">R$ {product.price.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</p>
          {product.original_price && <p className="text-sm text-gray-300 line-through mt-1">De R$ {product.original_price}</p>}
        </div>

        <div className="space-y-3">
           <h3 className="text-lg font-bold">Sobre este item</h3>
           <p className="text-gray-500 text-sm leading-relaxed">
             {product.description || "Este produto premium oferece qualidade superior e design moderno, ideal para quem busca o melhor custo-benefício do mercado."}
           </p>
        </div>

        <div className="grid grid-cols-2 gap-4 pb-32">
           <div className="bg-white p-4 rounded-3xl border border-gray-100 shadow-sm">
              <p className="text-[10px] text-gray-400 font-bold uppercase mb-1">Estoque</p>
              <p className="text-sm font-black">Disponível</p>
           </div>
           <div className="bg-white p-4 rounded-3xl border border-gray-100 shadow-sm">
              <p className="text-[10px] text-gray-400 font-bold uppercase mb-1">Envio</p>
              <p className="text-sm font-black text-emerald-500">Grátis</p>
           </div>
        </div>
      </div>

      {/* Bottom Sticky Action */}
      <div className="fixed bottom-0 left-0 right-0 max-w-md mx-auto p-6 bg-white/80 backdrop-blur-xl border-t border-gray-100 flex gap-4 z-[90]">
        <button
          onClick={() => { onAddToCart(product); onBack(); }}
          className="flex-1 bg-[#2D3B87] text-white font-black py-5 rounded-[24px] shadow-xl shadow-blue-100 uppercase tracking-widest active:scale-95 transition-transform"
        >
          Comprar Agora
        </button>
      </div>
    </div>
  );
};

export default ProductDetails;
