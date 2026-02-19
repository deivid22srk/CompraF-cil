
import React, { useEffect, useState } from 'react';
import { ArrowLeft, User, Package, MapPin, ChevronRight, LogOut, Loader2, Bell } from 'lucide-react';
import { supabase } from '../supabaseClient';
import { Order } from '../types';
import { User as SupabaseUser } from '@supabase/supabase-js';

interface ProfileViewProps {
  user: SupabaseUser | null;
  onBack: () => void;
  onAuthClick: () => void;
}

const ProfileView: React.FC<ProfileViewProps> = ({ user, onBack, onAuthClick }) => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user) {
      const fetchOrders = async () => {
        const { data } = await supabase
          .from('orders')
          .select('*')
          .eq('user_id', user.id)
          .order('created_at', { ascending: false });
        if (data) setOrders(data);
        setLoading(false);
      };
      fetchOrders();
    } else {
      setLoading(false);
    }
  }, [user]);

  const handleLogout = async () => {
    await supabase.auth.signOut();
  };

  if (!user) {
    return (
      <div className="min-h-screen bg-slate-50 p-6 flex flex-col items-center justify-center text-center">
        <div className="w-20 h-20 bg-white rounded-3xl shadow-sm flex items-center justify-center mb-6">
          <User size={32} className="text-slate-200" />
        </div>
        <h2 className="text-xl font-black mb-2">Ops! Você não está logado</h2>
        <p className="text-slate-400 text-sm mb-8 px-8">Entre para acompanhar seus pedidos e gerenciar sua conta.</p>
        <button
          onClick={onAuthClick}
          className="w-full bg-indigo-600 text-white font-black py-4 rounded-3xl shadow-xl shadow-indigo-100 uppercase tracking-widest"
        >
          Fazer Login
        </button>
        <button onClick={onBack} className="mt-6 text-sm font-bold text-slate-400">Voltar para a Loja</button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 pb-12">
      {/* Header Profile */}
      <div className="bg-indigo-600 p-8 pt-12 rounded-b-[48px] text-white relative">
        <button onClick={onBack} className="absolute top-8 left-4 p-2 hover:bg-white/10 rounded-xl transition-colors">
          <ArrowLeft size={20} />
        </button>
        <div className="flex items-center gap-4 mb-6">
          <div className="w-20 h-20 rounded-3xl bg-white/20 backdrop-blur-md flex items-center justify-center text-3xl shadow-2xl">
            👤
          </div>
          <div>
            <h2 className="text-xl font-black">{user.email?.split('@')[0]}</h2>
            <p className="text-xs text-white/60">{user.email}</p>
          </div>
          <button onClick={handleLogout} className="ml-auto p-3 bg-white/10 rounded-2xl text-white">
            <LogOut size={20} />
          </button>
        </div>
        <div className="flex justify-between items-center bg-white/10 backdrop-blur-md p-4 rounded-3xl">
          <div className="text-center border-r border-white/10 flex-1">
            <p className="text-[10px] text-white/50 font-bold uppercase mb-1">Meus Pedidos</p>
            <p className="text-lg font-black">{orders.length}</p>
          </div>
          <div className="text-center flex-1">
            <p className="text-[10px] text-white/50 font-bold uppercase mb-1">Carteira</p>
            <p className="text-lg font-black">R$ 0,00</p>
          </div>
        </div>
      </div>

      <div className="p-6 -mt-6">
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-black text-slate-800">Rastreio de Pedidos</h3>
          <button className="p-2 bg-white rounded-xl shadow-sm"><Bell size={18} className="text-indigo-600" /></button>
        </div>

        {loading ? (
          <div className="flex justify-center py-20"><Loader2 className="animate-spin text-indigo-600" /></div>
        ) : orders.length === 0 ? (
          <div className="bg-white p-8 rounded-[32px] shadow-sm text-center">
            <Package size={40} className="text-slate-100 mx-auto mb-4" />
            <p className="text-xs font-bold text-slate-400">Você ainda não tem nenhum pedido em trânsito.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {orders.map(order => (
              <div key={order.id} className="bg-white p-5 rounded-[32px] shadow-sm border border-slate-50 group">
                <div className="flex justify-between items-start mb-4">
                  <div>
                    <span className={`text-[10px] font-black uppercase px-2 py-1 rounded-lg ${
                      order.status === 'Entregue' ? 'bg-emerald-100 text-emerald-600' : 'bg-indigo-100 text-indigo-600'
                    }`}>
                      {order.status}
                    </span>
                    <h4 className="text-sm font-bold mt-2 text-slate-800 line-clamp-1">{order.product_names}</h4>
                  </div>
                  <p className="text-xs font-black text-slate-300">#{order.id.slice(0, 8)}</p>
                </div>

                <div className="space-y-3">
                  <div className="flex gap-3">
                    <div className="flex flex-col items-center">
                      <div className="w-2 h-2 rounded-full bg-indigo-600"></div>
                      <div className="w-0.5 h-6 bg-slate-100 my-1"></div>
                      <div className="w-2 h-2 rounded-full bg-slate-200"></div>
                    </div>
                    <div className="flex-1">
                      <div className="flex justify-between items-start mb-4">
                        <div className="flex gap-2">
                           <MapPin size={14} className="text-indigo-600" />
                           <div>
                             <p className="text-[10px] text-slate-400 font-bold uppercase">Localização Atual</p>
                             <p className="text-xs font-bold text-slate-800">{order.last_location || 'Aguardando atualização'}</p>
                           </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                {order.tracking_code && (
                  <div className="mt-4 pt-4 border-t border-slate-50 flex justify-between items-center">
                    <div className="text-[10px] font-bold text-slate-400 tracking-widest uppercase">
                      Cod: {order.tracking_code}
                    </div>
                    <button className="text-xs font-bold text-indigo-600 flex items-center gap-1">
                      Ver no Mapa <ChevronRight size={14} />
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default ProfileView;
