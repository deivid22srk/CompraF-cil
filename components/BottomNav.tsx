
import React from 'react';
import { Home, LayoutGrid, ShoppingBag, User, Lock } from 'lucide-react';
import { View } from '../types';

interface BottomNavProps {
  activeView: View;
  onNavigate: (view: View) => void;
  cartCount: number;
  isGuest: boolean;
}

const BottomNav: React.FC<BottomNavProps> = ({ activeView, onNavigate, cartCount, isGuest }) => {
  return (
    <div className="fixed bottom-0 left-0 right-0 max-w-md mx-auto h-24 bg-white px-10 flex items-center justify-between z-[100] border-t border-slate-50">
      <button
        onClick={() => onNavigate('home')}
        className={`flex flex-col items-center gap-1 transition-all ${activeView === 'home' ? 'text-[#FDCB58]' : 'text-slate-300'}`}
      >
        <Home size={26} fill={activeView === 'home' ? '#FDCB58' : 'none'} />
      </button>

      <button
        onClick={() => onNavigate('cart')}
        className={`flex flex-col items-center gap-1 transition-all ${activeView === 'cart' ? 'text-[#FDCB58]' : 'text-slate-300'} relative`}
      >
        <ShoppingBag size={26} fill={activeView === 'cart' ? '#FDCB58' : 'none'} />
        {cartCount > 0 && (
          <span className="absolute -top-1 -right-1 bg-red-500 text-white text-[8px] font-black w-4 h-4 rounded-full flex items-center justify-center border-2 border-white">
            {cartCount}
          </span>
        )}
      </button>

      <button
        onClick={() => onNavigate('profile')}
        className={`flex flex-col items-center gap-1 transition-all ${activeView === 'profile' ? 'text-[#FDCB58]' : 'text-slate-300'}`}
      >
        <User size={26} fill={activeView === 'profile' ? '#FDCB58' : 'none'} />
      </button>

      <button
        onClick={() => onNavigate('admin')}
        className={`flex flex-col items-center gap-1 transition-all ${activeView === 'admin' ? 'text-[#FDCB58]' : 'text-slate-300'}`}
      >
        {isGuest ? <Lock size={26} /> : <LayoutGrid size={26} fill={activeView === 'admin' ? '#FDCB58' : 'none'} />}
      </button>
    </div>
  );
};

export default BottomNav;
