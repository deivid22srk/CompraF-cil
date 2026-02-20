
import React from 'react';
import { ArrowLeft, X, Minus, Plus, CreditCard, MapPin } from 'lucide-react';
import { CartItem } from '../types';

interface CartViewProps {
  cart: CartItem[];
  onUpdateQuantity: (id: string, delta: number) => void;
  onBack: () => void;
}

const CartView: React.FC<CartViewProps> = ({ cart, onUpdateQuantity, onBack }) => {
  const subtotal = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);
  const shipping = cart.length > 0 ? 0 : 0; // Free shipping for elegance
  const total = subtotal + shipping;

  return (
    <div className="animate-in fade-in duration-500 p-6 min-h-screen bg-[#F8F9FA] text-gray-900 pb-32">
      <div className="flex items-center justify-between mb-10 pt-4">
        <button onClick={onBack} className="w-12 h-12 rounded-full bg-white shadow-sm flex items-center justify-center border border-gray-100">
          <ArrowLeft size={24} />
        </button>
        <h2 className="text-2xl font-black">Meu Carrinho</h2>
        <div className="w-12" />
      </div>

      {cart.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-32 text-gray-300">
          <div className="w-32 h-32 bg-white rounded-full flex items-center justify-center mb-6 shadow-sm border border-gray-100">
            <ShoppingBag size={48} className="text-gray-100" />
          </div>
          <p className="text-lg font-black text-gray-400">Seu carrinho está vazio</p>
          <button onClick={onBack} className="mt-4 text-[#2D3B87] font-bold">Continuar Comprando</button>
        </div>
      ) : (
        <div className="space-y-8">
          <div className="space-y-4">
            {cart.map(item => (
              <div key={item.id} className="flex gap-4 p-4 bg-white rounded-[32px] shadow-sm border border-gray-100 animate-in slide-in-from-bottom duration-300">
                 <div className="w-24 h-24 bg-[#F8F9FA] rounded-2xl p-2 flex items-center justify-center overflow-hidden">
                   <img src={item.image_url} alt={item.name} className="w-full h-full object-contain" />
                 </div>
                 <div className="flex-1 flex flex-col justify-center">
                   <h4 className="text-sm font-black text-gray-900 pr-8 leading-tight">{item.name}</h4>
                   <p className="text-[#E67E22] font-black text-lg mt-1">R$ {item.price.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</p>

                   <div className="flex items-center gap-4 mt-3">
                     <button onClick={() => onUpdateQuantity(item.id, -1)} className="w-8 h-8 rounded-full bg-gray-50 flex items-center justify-center border border-gray-100"><Minus size={14} /></button>
                     <span className="text-sm font-black w-4 text-center">{item.quantity}</span>
                     <button onClick={() => onUpdateQuantity(item.id, 1)} className="w-8 h-8 rounded-full bg-[#2D3B87] text-white flex items-center justify-center shadow-md"><Plus size={14} /></button>
                   </div>
                 </div>
                 <button
                   onClick={() => onUpdateQuantity(item.id, -item.quantity)}
                   className="absolute top-4 right-4 text-gray-300 hover:text-red-400"
                  >
                    <X size={20} />
                  </button>
              </div>
            ))}
          </div>

          {/* Delivery & Totals Summary */}
          <div className="bg-white rounded-[40px] p-8 shadow-xl border border-gray-50 space-y-6">
            <div className="space-y-4">
               <div className="flex justify-between items-center">
                 <span className="text-sm font-bold text-gray-400 uppercase tracking-widest">Subtotal</span>
                 <span className="text-lg font-bold">R$ {subtotal.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</span>
               </div>
               <div className="flex justify-between items-center">
                 <span className="text-sm font-bold text-gray-400 uppercase tracking-widest">Entrega</span>
                 <span className="text-lg font-bold text-emerald-500">Grátis</span>
               </div>
               <div className="pt-4 border-t border-gray-50 flex justify-between items-center">
                 <span className="text-xl font-black">Total</span>
                 <span className="text-3xl font-black text-[#E67E22]">R$ {total.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</span>
               </div>
            </div>

            <button className="w-full bg-[#2D3B87] text-white font-black py-5 rounded-[24px] shadow-2xl shadow-blue-100 uppercase tracking-widest active:scale-95 transition-transform flex items-center justify-center gap-3">
              Finalizar Compra <CreditCard size={20} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default CartView;
