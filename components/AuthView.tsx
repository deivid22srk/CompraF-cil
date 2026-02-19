
import React, { useState } from 'react';
import { supabase } from '../supabaseClient';
import { Mail, Lock, ArrowLeft, Loader2 } from 'lucide-react';

interface AuthViewProps {
  onBack: () => void;
}

const AuthView: React.FC<AuthViewProps> = ({ onBack }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLogin, setIsLogin] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    const { error: authError } = isLogin
      ? await supabase.auth.signInWithPassword({ email, password })
      : await supabase.auth.signUp({ email, password });

    if (authError) {
      setError(authError.message);
      setLoading(false);
    }
  };

  return (
    <div className="p-8 bg-white min-h-screen animate-in fade-in duration-500">
      <button onClick={onBack} className="w-12 h-12 bg-slate-50 rounded-2xl flex items-center justify-center mb-12">
        <ArrowLeft size={24} />
      </button>

      <div className="mb-12">
        <h1 className="text-4xl font-black text-slate-900 mb-2">
          {isLogin ? 'Bem-vindo!' : 'Crie sua conta'}
        </h1>
        <p className="text-slate-400">Acesse o painel administrativo do CompraFácil.</p>
      </div>

      <form onSubmit={handleAuth} className="space-y-6">
        <div className="relative">
          <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-300" size={20} />
          <input
            type="email"
            placeholder="E-mail"
            className="w-full pl-12 pr-4 py-4 bg-slate-50 rounded-2xl border-none focus:ring-2 focus:ring-indigo-500"
            value={email}
            onChange={e => setEmail(e.target.value)}
            required
          />
        </div>

        <div className="relative">
          <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-300" size={20} />
          <input
            type="password"
            placeholder="Senha"
            className="w-full pl-12 pr-4 py-4 bg-slate-50 rounded-2xl border-none focus:ring-2 focus:ring-indigo-500"
            value={password}
            onChange={e => setPassword(e.target.value)}
            required
          />
        </div>

        {error && <p className="text-red-500 text-xs font-bold text-center">{error}</p>}

        <button
          type="submit"
          disabled={loading}
          className="w-full bg-indigo-600 text-white font-black py-4 rounded-3xl shadow-xl shadow-indigo-100 uppercase tracking-widest flex items-center justify-center gap-2"
        >
          {loading ? <Loader2 className="animate-spin" /> : (isLogin ? 'Entrar' : 'Cadastrar')}
        </button>
      </form>

      <div className="mt-8 text-center">
        <button onClick={() => setIsLogin(!isLogin)} className="text-sm font-bold text-slate-400">
          {isLogin ? 'Não tem conta? Cadastre-se' : 'Já tem conta? Faça login'}
        </button>
      </div>
    </div>
  );
};

export default AuthView;
