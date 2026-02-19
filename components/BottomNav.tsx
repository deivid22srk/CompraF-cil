
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
    <div className="fixed bottom-0 left-0 right-0 max-w-md mx-auto h-20 bg-white/90 backdrop-blur-xl border-t border-slate-100 px-6 flex items-center justify-between z-50">
      <button
        onClick={() => onNavigate('home')}
        className={`flex flex-col items-center gap-1 transition-all ${activeView === 'home' ? 'text-indigo-600 scale-110' : 'text-slate-300'}`}
      >
        <Home size={22} />
        <span className="text-[10px] font-bold">Início</span>
      </button>

      <button
        className={`flex flex-col items-center gap-1 transition-all ${activeView === 'admin' ? 'text-indigo-600 scale-110' : 'text-slate-300'} relative`}
        onClick={() => onNavigate('admin')}
      >
        {isGuest ? (
          <div className="flex flex-col items-center gap-1 opacity-60">
            <Lock size={22} />
            <span className="text-[10px] font-bold">Admin</span>
          </div>
        ) : (
          <>
            <LayoutGrid size={22} />
            <span className="text-[10px] font-bold">Admin</span>
          </>
        )}
      </button>

      {/* Floating Center Icon */}
      <div className="relative -top-6">
        <button
          onClick={() => onNavigate('cart')}
          className="w-16 h-16 rounded-full bg-indigo-600 text-white shadow-xl shadow-indigo-200 flex items-center justify-center hover:scale-110 active:scale-95 transition-transform"
        >
          <div className="relative">
            <ShoppingBag size={24} />
            {cartCount > 0 && (
              <span className="absolute -top-1 -right-1 bg-white text-indigo-600 text-[8px] font-black w-4 h-4 rounded-full flex items-center justify-center">
                {cartCount}
              </span>
            )}
          </div>
        </button>
      </div>

      <button
        onClick={() => onNavigate('profile')}
        className={`flex flex-col items-center gap-1 transition-all ${activeView === 'profile' ? 'text-indigo-600 scale-110' : 'text-slate-300'}`}
      >
        <User size={22} />
        <span className="text-[10px] font-bold">Perfil</span>
      </button>

      <div className="flex flex-col items-center gap-1 text-slate-300">
        <div className="w-6 h-6 rounded bg-slate-100 flex items-center justify-center text-[8px] font-bold text-slate-400">CF</div>
        <span className="text-[10px] font-bold">Menu</span>
      </div>
    </div>
  );
};

export default BottomNav;
