import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { supabase } from '../supabaseClient'
import { CheckCircle2, AlertCircle, Loader2 } from 'lucide-react'

export default function AuthCallback() {
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading')
  const [message, setMessage] = useState('Processando confirmação...')
  const navigate = useNavigate()

  useEffect(() => {
    // Supabase handles the session exchange automatically if redirected here with a fragment
    const handleAuth = async () => {
        const { error } = await supabase.auth.getSession()
        if (error) {
            setStatus('error')
            setMessage('Não foi possível verificar seu acesso: ' + error.message)
        }
    }

    handleAuth()

    const { data: authListener } = supabase.auth.onAuthStateChange((event) => {
      if (event === 'SIGNED_IN') {
          setStatus('success')
          setMessage('Email confirmado com sucesso! Você já pode fechar esta aba e retornar ao aplicativo.')
      } else if (event === 'PASSWORD_RECOVERY') {
          setStatus('success')
          setMessage('Recuperação de senha iniciada. Verifique seu app para concluir.')
      } else if (event === 'USER_UPDATED') {
          setStatus('success')
          setMessage('Sua conta foi atualizada.')
      }
    })

    return () => {
      authListener.subscription.unsubscribe()
    }
  }, [navigate])

  return (
    <div className="min-h-screen flex items-center justify-center p-6 text-center">
      <div className="bg-card p-10 rounded-[40px] shadow-2xl max-w-md w-full border border-white/5">
        {status === 'loading' && (
          <Loader2 className="w-16 h-16 text-primary animate-spin mx-auto mb-6" />
        )}
        {status === 'success' && (
          <CheckCircle2 className="w-16 h-16 text-green-500 mx-auto mb-6" />
        )}
        {status === 'error' && (
          <AlertCircle className="w-16 h-16 text-red-500 mx-auto mb-6" />
        )}

        <h2 className="text-2xl font-bold mb-4">
          {status === 'success' ? 'Sucesso!' : status === 'error' ? 'Ops!' : 'Aguarde...'}
        </h2>
        <p className="text-gray-400">
          {message}
        </p>

        {status !== 'loading' && (
          <button
            onClick={() => navigate('/')}
            className="mt-8 w-full py-4 bg-primary text-black font-black rounded-2xl hover:bg-secondary transition-colors"
          >
            IR PARA A LOJA
          </button>
        )}
      </div>
    </div>
  )
}
