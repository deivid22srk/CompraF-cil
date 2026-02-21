import { HashRouter as Router, Routes, Route } from 'react-router-dom'
import Home from './pages/Home'
import ProductDetails from './pages/ProductDetails'
import AuthCallback from './pages/AuthCallback'

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-background text-white">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/product/:id" element={<ProductDetails />} />
          <Route path="/confirm" element={<AuthCallback />} />
          <Route path="/reset-password" element={<AuthCallback />} />
        </Routes>
      </div>
    </Router>
  )
}

export default App
