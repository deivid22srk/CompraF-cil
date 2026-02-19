
import React from 'react';
import { ShoppingBag, ArrowRight, UserCircle2, ShieldCheck } from 'lucide-react';

interface WelcomeViewProps {
  onStartAsGuest: () => void;
  onGoToAuth: () => void;
}

const WelcomeView: React.FC<WelcomeViewProps> = ({ onStartAsGuest, onGoToAuth }) => {
  return (
    <div className="min-h-screen bg-white flex flex-col p-8 justify-between animate-in fade-in duration-700">
      <div className="mt-20 text-center">
        <div className="w-24 h-24 bg-indigo-600 rounded-[32px] flex items-center justify-center mx-auto mb-8 shadow-2xl shadow-indigo-200 animate-bounce">
          <ShoppingBag size={40} className="text-white" />
        </div>
        <h1 className="text-4xl font-black text-slate-900 mb-4 tracking-tight">CompraFácil</h1>
        <p className="text-slate-400 max-w-[250px] mx-auto text-sm leading-relaxed">
          Sua loja de produtos favoritos, agora na palma da sua mão.
        </p>
      </div>

      <div className="space-y-4 mb-8">
        <button
          onClick={onGoToAuth}
          className="w-full bg-indigo-600 text-white font-black py-4 rounded-3xl shadow-xl shadow-indigo-100 flex items-center justify-center gap-3 transition-transform active:scale-95 group"
        >
          <UserCircle2 size={20} />
          Começar Agora
          <ArrowRight size={18} className="group-hover:translate-x-1 transition-transform" />
        </button>

        <button
          onClick={onStartAsGuest}
          className="w-full bg-slate-50 text-slate-600 font-bold py-4 rounded-3xl flex items-center justify-center gap-3 transition-transform active:scale-95"
        >
          Entrar como Visitante
        </button>

        <div className="pt-4 flex items-center justify-center gap-2 text-[10px] text-slate-300 font-bold uppercase tracking-widest">
          <ShieldCheck size={12} />
          Pagamento 100% Seguro
        </div>
      </div>
    </div>
  );
};

export default WelcomeView;
