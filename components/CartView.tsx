
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
  const shipping = cart.length > 0 ? 15.00 : 0;
  const total = subtotal + shipping;

  return (
    <div className="animate-in fade-in duration-500 p-4">
      <div className="flex items-center justify-between mb-8 pt-4">
        <button onClick={onBack} className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center">
          <ArrowLeft size={20} />
        </button>
        <h2 className="text-xl font-bold">Cart</h2>
        <div className="w-10" />
      </div>

      {cart.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-slate-300">
          <div className="w-24 h-24 bg-slate-100 rounded-full flex items-center justify-center mb-4">
            <X size={40} />
          </div>
          <p className="font-bold">Your cart is empty</p>
        </div>
      ) : (
        <div className="space-y-6">
          {cart.map(item => (
            <div key={item.id} className="flex gap-4 p-2 bg-white rounded-3xl relative">
               <div className="w-20 h-20 bg-slate-50 rounded-2xl p-2 flex items-center justify-center">
                 <img src={item.image_url} alt={item.name} className="w-full h-full object-contain" />
               </div>
               <div className="flex-1 flex flex-col justify-center">
                 <h4 className="text-sm font-bold text-slate-800 pr-8">{item.name}</h4>
                 <p className="text-indigo-600 font-black text-sm mt-1">R$ {item.price.toLocaleString('pt-BR')}</p>
               </div>
               <div className="flex items-center gap-2 bg-slate-50 rounded-xl p-1 h-fit my-auto">
                 <button onClick={() => onUpdateQuantity(item.id, -1)} className="w-6 h-6 rounded-lg bg-white shadow-sm flex items-center justify-center"><Minus size={12} /></button>
                 <span className="text-sm font-bold w-4 text-center">{item.quantity}</span>
                 <button onClick={() => onUpdateQuantity(item.id, 1)} className="w-6 h-6 rounded-lg bg-indigo-600 text-white flex items-center justify-center"><Plus size={12} /></button>
               </div>
               <button
                 onClick={() => onUpdateQuantity(item.id, -item.quantity)}
                 className="absolute top-2 right-2 text-slate-300 hover:text-red-400"
                >
                  <X size={18} />
                </button>
            </div>
          ))}

          {/* Delivery & Payment Info */}
          <div className="bg-white rounded-3xl p-4 shadow-sm space-y-4">
            <div className="flex gap-3">
              <div className="w-10 h-10 bg-slate-50 rounded-xl flex items-center justify-center text-slate-400">
                <MapPin size={20} />
              </div>
              <div className="flex-1">
                <p className="text-xs font-bold">Delivery to</p>
                <p className="text-[10px] text-slate-400">Rua das Flores, 123, São Paulo, SP</p>
              </div>
            </div>
            <hr className="border-slate-50" />
            <div className="flex gap-3">
              <div className="w-10 h-10 bg-slate-50 rounded-xl flex items-center justify-center text-slate-400">
                <CreditCard size={20} />
              </div>
              <div className="flex-1">
                <p className="text-xs font-bold">Payment Method</p>
                <div className="flex items-center gap-2">
                  <span className="w-4 h-3 bg-red-400 rounded-sm"></span>
                  <p className="text-[10px] text-slate-400">VISA Classic **** 9388</p>
                </div>
              </div>
            </div>
          </div>

          {/* Totals */}
          <div className="space-y-3 p-2">
             <div className="flex justify-between text-sm font-medium">
               <span className="text-slate-400">Sub total:</span>
               <span>R$ {subtotal.toLocaleString('pt-BR')}</span>
             </div>
             <div className="flex justify-between text-sm font-medium">
               <span className="text-slate-400">Shipping:</span>
               <span>R$ {shipping.toLocaleString('pt-BR')}</span>
             </div>
             <hr className="border-slate-100" />
             <div className="flex justify-between text-lg font-black">
               <span>Total:</span>
               <span className="text-indigo-600">R$ {total.toLocaleString('pt-BR')}</span>
             </div>
          </div>

          <button className="w-full bg-indigo-600 text-white font-black py-4 rounded-2xl shadow-xl shadow-indigo-200 uppercase tracking-widest mt-4">
            Proceed to Buy
          </button>
        </div>
      )}
    </div>
  );
};

export default CartView;
