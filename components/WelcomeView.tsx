
import React from 'react';
import { ShoppingBag, ArrowRight, UserCircle2, ShieldCheck } from 'lucide-react';

interface WelcomeViewProps {
  onStartAsGuest: () => void;
  onGoToAuth: () => void;
}

const WelcomeView: React.FC<WelcomeViewProps> = ({ onStartAsGuest, onGoToAuth }) => {
  return (
    <div className="min-h-screen bg-[#0F0F0F] flex flex-col p-10 justify-between animate-in fade-in duration-700 text-white">
      <div className="mt-24 text-center">
        <div className="w-28 h-28 bg-[#FDCB58] rounded-[48px] flex items-center justify-center mx-auto mb-10 shadow-2xl shadow-yellow-500/20 rotate-12">
          <ShoppingBag size={48} className="text-[#2D3B87]" fill="currentColor" />
        </div>
        <h1 className="text-5xl font-black mb-4 tracking-tighter">Compra Fácil</h1>
        <p className="text-gray-400 max-w-[280px] mx-auto text-md leading-relaxed">
          Sua nova experiência de compras online, rápida e segura.
        </p>
      </div>

      <div className="space-y-5 mb-10">
        <button
          onClick={onGoToAuth}
          className="w-full bg-[#2D3B87] text-white font-black py-5 rounded-[28px] shadow-xl shadow-blue-900/20 flex items-center justify-center gap-3 transition-transform active:scale-95 group"
        >
          <UserCircle2 size={22} />
          <span className="tracking-widest uppercase text-sm">Entrar na Conta</span>
          <ArrowRight size={20} className="group-hover:translate-x-2 transition-transform" />
        </button>

        <button
          onClick={onStartAsGuest}
          className="w-full bg-white/5 text-gray-300 font-bold py-5 rounded-[28px] flex items-center justify-center gap-3 transition-transform active:scale-95 border border-white/10"
        >
          Explorar como Visitante
        </button>

        <div className="pt-6 flex items-center justify-center gap-3 text-[10px] text-gray-500 font-black uppercase tracking-[0.2em]">
          <ShieldCheck size={14} className="text-emerald-500" />
          Segurança Garantida
        </div>
      </div>
    </div>
  );
};

export default WelcomeView;
