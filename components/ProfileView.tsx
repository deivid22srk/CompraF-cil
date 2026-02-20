
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
      <div className="min-h-screen bg-[#F8F9FA] p-8 flex flex-col items-center justify-center text-center">
        <div className="w-24 h-24 bg-white rounded-[40px] shadow-sm flex items-center justify-center mb-8 border border-gray-100">
          <User size={40} className="text-gray-200" />
        </div>
        <h2 className="text-2xl font-black mb-3">Sua conta</h2>
        <p className="text-gray-400 text-sm mb-10 px-6 leading-relaxed">Faça login para gerenciar seus pedidos e ter uma experiência personalizada.</p>
        <button
          onClick={onAuthClick}
          className="w-full bg-[#2D3B87] text-white font-black py-5 rounded-[24px] shadow-xl shadow-blue-100 uppercase tracking-widest active:scale-95 transition-transform"
        >
          Entrar Agora
        </button>
        <button onClick={onBack} className="mt-6 text-sm font-bold text-gray-300">Voltar para a Loja</button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F8F9FA] pb-32">
      {/* Header Profile */}
      <div className="bg-[#2D3B87] p-10 pt-16 rounded-b-[60px] text-white relative shadow-2xl">
        <button onClick={onBack} className="absolute top-8 left-6 p-2 bg-white/10 rounded-full transition-colors">
          <ArrowLeft size={20} />
        </button>

        <div className="flex flex-col items-center text-center">
          <div className="w-24 h-24 rounded-[40px] bg-white p-1 mb-4 shadow-xl">
             <div className="w-full h-full rounded-[36px] bg-[#FDCB58] flex items-center justify-center text-3xl">
               👤
             </div>
          </div>
          <h2 className="text-2xl font-black mb-1">{user.email?.split('@')[0]}</h2>
          <p className="text-sm text-white/50 mb-8">{user.email}</p>

          <div className="flex w-full gap-4">
             <div className="flex-1 bg-white/10 backdrop-blur-md rounded-3xl p-4">
                <p className="text-[10px] text-white/40 font-black uppercase mb-1">Pedidos</p>
                <p className="text-xl font-black">{orders.length}</p>
             </div>
             <div className="flex-1 bg-white/10 backdrop-blur-md rounded-3xl p-4">
                <p className="text-[10px] text-white/40 font-black uppercase mb-1">Créditos</p>
                <p className="text-xl font-black">R$ 0,00</p>
             </div>
          </div>
        </div>
      </div>

      <div className="p-8">
        <div className="flex justify-between items-center mb-8">
          <h3 className="text-xl font-black text-gray-900">Rastreio Ativo</h3>
          <button onClick={handleLogout} className="flex items-center gap-2 text-red-400 font-bold text-xs uppercase tracking-widest">
            Sair <LogOut size={16} />
          </button>
        </div>

        {loading ? (
          <div className="flex justify-center py-20"><Loader2 className="animate-spin text-[#2D3B87]" /></div>
        ) : orders.length === 0 ? (
          <div className="bg-white p-10 rounded-[40px] shadow-sm text-center border border-gray-50">
            <Package size={48} className="text-gray-100 mx-auto mb-4" />
            <p className="text-sm font-bold text-gray-300">Nenhum pedido em trânsito no momento.</p>
          </div>
        ) : (
          <div className="space-y-6">
            {orders.map(order => (
              <div key={order.id} className="bg-white p-6 rounded-[40px] shadow-sm border border-gray-50 animate-in slide-in-from-bottom duration-500">
                <div className="flex justify-between items-start mb-6">
                  <div>
                    <span className={`text-[10px] font-black uppercase px-3 py-1.5 rounded-full ${
                      order.status === 'Entregue' ? 'bg-emerald-50 text-emerald-500' : 'bg-blue-50 text-[#2D3B87]'
                    }`}>
                      {order.status}
                    </span>
                    <h4 className="text-md font-black mt-3 text-gray-900 line-clamp-1 leading-tight">{order.product_names}</h4>
                  </div>
                </div>

                <div className="flex items-center gap-4 bg-[#F8F9FA] p-4 rounded-3xl">
                   <div className="w-10 h-10 rounded-2xl bg-white flex items-center justify-center shadow-sm">
                      <MapPin size={20} className="text-[#E67E22]" />
                   </div>
                   <div className="flex-1">
                      <p className="text-[10px] text-gray-400 font-black uppercase">Última Parada</p>
                      <p className="text-xs font-bold text-gray-700">{order.last_location || 'Processando envio'}</p>
                   </div>
                   <ChevronRight size={20} className="text-gray-200" />
                </div>

                {order.tracking_code && (
                  <div className="mt-6 flex items-center justify-between bg-[#2D3B87] p-4 rounded-3xl text-white">
                    <div className="text-[10px] font-black uppercase opacity-60 tracking-widest">
                      {order.tracking_code}
                    </div>
                    <button className="text-[10px] font-black uppercase tracking-widest bg-white/20 px-3 py-1 rounded-lg">
                      Copiar
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
