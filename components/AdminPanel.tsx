
import React, { useState, useEffect } from 'react';
import { Plus, Trash2, LayoutGrid, Image as ImageIcon, Sparkles, LogOut, ArrowLeft, Package, Check, RefreshCw, User as UserIcon, Camera } from 'lucide-react';
import { supabase } from '../supabaseClient';
import { Product, Order } from '../types';
import { GoogleGenAI } from "@google/genai";
import { Camera as CapCamera, CameraResultType, CameraSource } from '@capacitor/camera';

interface AdminPanelProps {
  onBack: () => void;
}

const AdminPanel: React.FC<AdminPanelProps> = ({ onBack }) => {
  const [activeTab, setActiveTab] = useState<'products' | 'orders'>('products');
  const [products, setProducts] = useState<Product[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [isAdding, setIsAdding] = useState(false);
  const [loading, setLoading] = useState(false);
  const [aiGenerating, setAiGenerating] = useState(false);

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    price: '',
    original_price: '',
    category: 'Electronics',
    image_url: 'https://picsum.photos/seed/' + Math.random() + '/400/400'
  });

  const fetchProducts = async () => {
    const { data } = await supabase.from('products').select('*').order('created_at', { ascending: false });
    if (data) setProducts(data);
  };

  const fetchOrders = async () => {
    const { data } = await supabase.from('orders').select('*').order('created_at', { ascending: false });
    if (data) setOrders(data);
  };

  useEffect(() => {
    fetchProducts();
    fetchOrders();
  }, []);

  const handleLogout = async () => {
    await supabase.auth.signOut();
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    const { error } = await supabase.from('products').insert([{
      name: formData.name,
      description: formData.description,
      price: parseFloat(formData.price),
      original_price: formData.original_price ? parseFloat(formData.original_price) : null,
      category: formData.category,
      image_url: formData.image_url,
      rating: 5,
      rating_count: 0
    }]);

    if (!error) {
      setIsAdding(false);
      setFormData({ name: '', description: '', price: '', original_price: '', category: 'Electronics', image_url: '' });
      fetchProducts();
    }
    setLoading(false);
  };

  const handleDelete = async (id: string) => {
    if (confirm('Deseja excluir este produto?')) {
      await supabase.from('products').delete().eq('id', id);
      fetchProducts();
    }
  };

  const updateOrderStatus = async (id: string, newStatus: string, lastLocation: string) => {
    setLoading(true);
    await supabase.from('orders').update({
      status: newStatus,
      last_location: lastLocation,
      tracking_code: `BR${Math.floor(Math.random() * 90000000) + 10000000}SH`
    }).eq('id', id);
    fetchOrders();
    setLoading(false);
  };

  const uploadImage = async (uri: string) => {
    try {
      const response = await fetch(uri);
      const blob = await response.blob();
      const fileName = `${Date.now()}-${Math.random().toString(36).substring(7)}.jpg`;

      const { data, error } = await supabase.storage
        .from('product-images')
        .upload(fileName, blob);

      if (error) throw error;

      const { data: { publicUrl } } = supabase.storage
        .from('product-images')
        .getPublicUrl(fileName);

      return publicUrl;
    } catch (err) {
      console.error('Erro no upload:', err);
      return null;
    }
  };

  const pickImage = async (source: CameraSource) => {
    try {
      const image = await CapCamera.getPhoto({
        quality: 90,
        allowEditing: false,
        resultType: CameraResultType.Uri,
        source: source
      });

      if (image.webPath) {
        setLoading(true);
        const uploadedUrl = await uploadImage(image.webPath);
        if (uploadedUrl) {
          setFormData(prev => ({ ...prev, image_url: uploadedUrl }));
        }
        setLoading(false);
      }
    } catch (err) {
      console.error('Erro ao capturar imagem:', err);
      setLoading(false);
    }
  };

  const generateAiDescription = async () => {
    if (!formData.name) return alert('Dê um nome ao produto primeiro');
    setAiGenerating(true);
    try {
      const apiKey = (import.meta as any).env?.VITE_GEMINI_API_KEY || (process as any).env?.GEMINI_API_KEY || '';
      const genAI = new GoogleGenAI(apiKey);
      const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });

      const prompt = `Escreva uma descrição de marketing curta e atraente para o produto: ${formData.name}. Categoria: ${formData.category}. Em português do Brasil. Máximo de 150 caracteres.`;

      const result = await model.generateContent(prompt);
      const response = await result.response;
      const text = response.text();

      setFormData(prev => ({ ...prev, description: text || '' }));
    } catch (err) {
      console.error(err);
    } finally {
      setAiGenerating(false);
    }
  };

  return (
    <div className="p-6 bg-slate-50 min-h-screen pb-24">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-3">
          <button onClick={onBack} className="p-2 hover:bg-slate-200 rounded-full transition-colors">
            <ArrowLeft size={20} />
          </button>
          <div>
            <h1 className="text-2xl font-black text-slate-800">Admin</h1>
            <p className="text-xs text-slate-400">Controle total da loja</p>
          </div>
        </div>
        <button onClick={handleLogout} className="p-3 text-red-500 bg-white rounded-2xl shadow-sm">
          <LogOut size={20} />
        </button>
      </div>

      <div className="flex bg-slate-100 p-1 rounded-2xl mb-8">
        <button
          onClick={() => setActiveTab('products')}
          className={`flex-1 py-3 rounded-xl text-xs font-black uppercase tracking-widest transition-all ${activeTab === 'products' ? 'bg-white shadow-sm text-indigo-600' : 'text-slate-400'}`}
        >
          Produtos
        </button>
        <button
          onClick={() => setActiveTab('orders')}
          className={`flex-1 py-3 rounded-xl text-xs font-black uppercase tracking-widest transition-all ${activeTab === 'orders' ? 'bg-white shadow-sm text-indigo-600' : 'text-slate-400'}`}
        >
          Rastreios
        </button>
      </div>

      {activeTab === 'products' ? (
        <>
          {!isAdding ? (
            <div className="space-y-4">
              <button
                onClick={() => setIsAdding(true)}
                className="w-full bg-indigo-600 text-white p-4 rounded-2xl flex items-center justify-center gap-2 font-bold shadow-lg shadow-indigo-100"
              >
                <Plus size={20} /> Novo Produto
              </button>

              <div className="grid gap-4">
                {products.map(p => (
                  <div key={p.id} className="bg-white p-3 rounded-2xl shadow-sm flex items-center gap-4 group">
                    <div className="w-16 h-16 bg-slate-50 rounded-xl overflow-hidden flex items-center justify-center">
                      <img src={p.image_url} alt={p.name} className="w-full h-full object-contain" />
                    </div>
                    <div className="flex-1">
                      <h4 className="text-sm font-bold line-clamp-1">{p.name}</h4>
                      <p className="text-xs text-indigo-600 font-bold">R$ {p.price}</p>
                    </div>
                    <button onClick={() => handleDelete(p.id)} className="p-2 text-slate-300 hover:text-red-500 transition-colors">
                      <Trash2 size={18} />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <form onSubmit={handleSave} className="bg-white p-6 rounded-[32px] shadow-sm space-y-4 animate-in slide-in-from-bottom duration-300">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-bold">Novo Produto</h2>
                <button type="button" onClick={() => setIsAdding(false)} className="text-slate-400">Cancelar</button>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-bold text-slate-400 uppercase">Nome</label>
                <input
                  required
                  className="w-full p-4 bg-slate-50 rounded-2xl border-none focus:ring-2 focus:ring-indigo-500"
                  value={formData.name}
                  onChange={e => setFormData({...formData, name: e.target.value})}
                  placeholder="Ex: Tênis Nike Air"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-xs font-bold text-slate-400 uppercase">Preço</label>
                  <input
                    required type="number" step="0.01"
                    className="w-full p-4 bg-slate-50 rounded-2xl border-none focus:ring-2 focus:ring-indigo-500"
                    value={formData.price}
                    onChange={e => setFormData({...formData, price: e.target.value})}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-xs font-bold text-slate-400 uppercase">Preço Antigo</label>
                  <input
                    type="number" step="0.01"
                    className="w-full p-4 bg-slate-50 rounded-2xl border-none focus:ring-2 focus:ring-indigo-500"
                    value={formData.original_price}
                    onChange={e => setFormData({...formData, original_price: e.target.value})}
                  />
                </div>
              </div>

              <div className="space-y-2">
                <div className="flex justify-between">
                  <label className="text-xs font-bold text-slate-400 uppercase">Descrição</label>
                  <button
                    type="button"
                    onClick={generateAiDescription}
                    disabled={aiGenerating}
                    className="text-[10px] flex items-center gap-1 text-indigo-600 font-bold bg-indigo-50 px-2 py-0.5 rounded-lg hover:bg-indigo-100 disabled:opacity-50"
                  >
                    <Sparkles size={10} /> {aiGenerating ? 'Gerando...' : 'IA Gerar'}
                  </button>
                </div>
                <textarea
                  rows={3}
                  className="w-full p-4 bg-slate-50 rounded-2xl border-none focus:ring-2 focus:ring-indigo-500"
                  value={formData.description}
                  onChange={e => setFormData({...formData, description: e.target.value})}
                />
              </div>

              <div className="space-y-2">
                <label className="text-xs font-bold text-slate-400 uppercase">Imagem do Produto</label>
                <div className="flex gap-2">
                  <input
                    required
                    className="flex-1 p-4 bg-slate-50 rounded-2xl border-none focus:ring-2 focus:ring-indigo-500 text-xs"
                    value={formData.image_url}
                    onChange={e => setFormData({...formData, image_url: e.target.value})}
                    placeholder="URL ou use os botões ao lado"
                  />
                  <button
                    type="button"
                    onClick={() => pickImage(CameraSource.Camera)}
                    className="p-4 bg-slate-100 rounded-2xl text-slate-600 hover:bg-slate-200"
                    title="Tirar Foto"
                  >
                    <Camera size={20} />
                  </button>
                  <button
                    type="button"
                    onClick={() => pickImage(CameraSource.Photos)}
                    className="p-4 bg-slate-100 rounded-2xl text-slate-600 hover:bg-slate-200"
                    title="Galeria"
                  >
                    <ImageIcon size={20} />
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-slate-900 text-white font-black py-4 rounded-2xl shadow-xl shadow-slate-200 uppercase tracking-widest mt-6 disabled:opacity-50"
              >
                {loading ? 'Salvando...' : 'Salvar Produto'}
              </button>
            </form>
          )}
        </>
      ) : (
        <div className="space-y-6">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xl font-bold flex items-center gap-2">
              <Package size={20} className="text-indigo-600" /> Gerenciar Pedidos
            </h2>
            <button onClick={fetchOrders} className="p-2 bg-white rounded-xl shadow-sm text-slate-400">
              <RefreshCw size={18} />
            </button>
          </div>

          <div className="space-y-4">
            {orders.length === 0 ? (
              <p className="text-center text-slate-400 py-10">Nenhum pedido realizado ainda.</p>
            ) : (
              orders.map(order => (
                <div key={order.id} className="bg-white p-6 rounded-[32px] shadow-sm border border-slate-50 space-y-4">
                  <div className="flex justify-between items-start">
                    <div>
                       <div className="flex items-center gap-2 text-indigo-600 mb-1">
                          <UserIcon size={14} />
                          <p className="text-[10px] font-black uppercase">{order.user_email}</p>
                       </div>
                       <h4 className="text-sm font-bold text-slate-800">{order.product_names}</h4>
                    </div>
                    <span className="text-[10px] font-black text-slate-300">#{order.id.slice(0, 8)}</span>
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                     <button
                        onClick={() => updateOrderStatus(order.id, 'Enviado', 'Centro Logístico BR')}
                        className={`p-3 rounded-2xl text-[10px] font-black uppercase flex items-center justify-center gap-2 border-2 transition-all ${order.status === 'Enviado' ? 'bg-indigo-600 border-indigo-600 text-white' : 'border-slate-50 text-slate-400'}`}
                     >
                       Enviado
                     </button>
                     <button
                        onClick={() => updateOrderStatus(order.id, 'Em Trânsito', 'Rumo à Cidade de Entrega')}
                        className={`p-3 rounded-2xl text-[10px] font-black uppercase flex items-center justify-center gap-2 border-2 transition-all ${order.status === 'Em Trânsito' ? 'bg-indigo-600 border-indigo-600 text-white' : 'border-slate-50 text-slate-400'}`}
                     >
                       Em Trânsito
                     </button>
                  </div>

                  <button
                     onClick={() => updateOrderStatus(order.id, 'Entregue', 'Endereço do Cliente')}
                     className={`w-full p-4 rounded-2xl text-xs font-black uppercase flex items-center justify-center gap-2 border-2 transition-all ${order.status === 'Entregue' ? 'bg-emerald-500 border-emerald-500 text-white' : 'border-slate-50 text-slate-400'}`}
                  >
                    {order.status === 'Entregue' ? <Check size={16} /> : null}
                    Marcar como Entregue
                  </button>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminPanel;
