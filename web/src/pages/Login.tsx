import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Mail, Lock, User, ArrowRight, Loader2 } from 'lucide-react'
import { supabase } from '../supabaseClient'

export default function Login() {
  const navigate = useNavigate()
  const [isRegister, setIsRegister] = useState(false)
  const [loading, setLoading] = useState(false)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)

    try {
      if (isRegister) {
        const { data, error: signUpError } = await supabase.auth.signUp({
          email,
          password,
          options: {
            data: { full_name: name },
            emailRedirectTo: `${window.location.origin}/#/confirm`
          }
        })
        if (signUpError) throw signUpError
        if (data.user) {
          // Profile is created by trigger in DB, but we can update it if needed
          alert('Cadastro realizado! Verifique seu e-mail para confirmar.')
          setIsRegister(false)
        }
      } else {
        const { error: signInError } = await supabase.auth.signInWithPassword({
          email,
          password
        })
        if (signInError) throw signInError
        navigate('/')
      }
    } catch (err: any) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-[calc(100vh-80px)] flex items-center justify-center p-6">
      <div className="w-full max-w-md bg-card p-8 md:p-12 rounded-[3rem] border border-white/5 shadow-2xl">
        <div className="text-center mb-10">
          <h1 className="text-4xl font-black mb-2">
            {isRegister ? 'Criar Conta' : 'Boas-vindas'}
          </h1>
          <p className="text-gray-400">
            {isRegister
              ? 'Preencha seus dados para começar a comprar'
              : 'Faça login para gerenciar seus pedidos e carrinho'}
          </p>
        </div>

        {error && (
          <div className="bg-red-500/10 border border-red-500/20 text-red-500 p-4 rounded-2xl mb-8 text-sm font-bold">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {isRegister && (
            <div className="space-y-2">
              <label className="text-xs font-black text-gray-500 uppercase tracking-widest px-1">Nome Completo</label>
              <div className="relative">
                <User className="absolute left-5 top-1/2 -translate-y-1/2 text-gray-500 w-5 h-5" />
                <input
                  type="text"
                  required
                  className="w-full pl-14 pr-6 py-4 bg-surface rounded-2xl border border-white/5 focus:border-primary outline-none transition-all font-medium"
                  placeholder="Seu nome"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>
            </div>
          )}

          <div className="space-y-2">
            <label className="text-xs font-black text-gray-500 uppercase tracking-widest px-1">E-mail</label>
            <div className="relative">
              <Mail className="absolute left-5 top-1/2 -translate-y-1/2 text-gray-500 w-5 h-5" />
              <input
                type="email"
                required
                className="w-full pl-14 pr-6 py-4 bg-surface rounded-2xl border border-white/5 focus:border-primary outline-none transition-all font-medium"
                placeholder="exemplo@email.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-xs font-black text-gray-500 uppercase tracking-widest px-1">Senha</label>
            <div className="relative">
              <Lock className="absolute left-5 top-1/2 -translate-y-1/2 text-gray-500 w-5 h-5" />
              <input
                type="password"
                required
                className="w-full pl-14 pr-6 py-4 bg-surface rounded-2xl border border-white/5 focus:border-primary outline-none transition-all font-medium"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-primary text-black py-5 rounded-2xl font-black flex items-center justify-center gap-2 hover:scale-[1.02] active:scale-95 transition-all disabled:opacity-50 disabled:scale-100"
          >
            {loading ? (
              <Loader2 className="animate-spin" />
            ) : (
              <>
                {isRegister ? 'CADASTRAR AGORA' : 'ENTRAR NO APP'}
                <ArrowRight size={20} />
              </>
            )}
          </button>
        </form>

        <div className="mt-10 text-center">
          <button
            onClick={() => setIsRegister(!isRegister)}
            className="text-primary font-bold hover:underline"
          >
            {isRegister ? 'Já tenho uma conta? Entrar' : 'Não tem conta? Cadastrar-se'}
          </button>
        </div>
      </div>
    </div>
  )
}
