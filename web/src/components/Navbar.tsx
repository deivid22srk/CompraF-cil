import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Download } from 'lucide-react'
import { configService } from '../services/configService'

export default function Navbar() {
  const [downloadUrl, setDownloadUrl] = useState('')

  useEffect(() => {
    configService.getDownloadUrl().then(url => {
      if (url) setDownloadUrl(url)
    })
  }, [])

  return (
    <nav className="bg-background border-b border-white/5 sticky top-0 z-[60]">
      <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
        <Link to="/" className="text-xl font-black text-primary italic tracking-tighter uppercase">CompraFacil</Link>

        <div className="flex items-center gap-4">
          {downloadUrl && (
            <a
              href={downloadUrl}
              target="_blank"
              rel="noreferrer"
              className="bg-primary text-black px-4 py-2 rounded-xl text-xs font-black hover:scale-105 transition-all flex items-center gap-2 shadow-lg shadow-primary/20"
            >
              <Download size={16} /> BAIXAR APP
            </a>
          )}
        </div>
      </div>
    </nav>
  )
}
