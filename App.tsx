
import React, { useState, useEffect } from 'react';
import { supabase } from './supabaseClient';
import { Product, CartItem, View } from './types';
import ShopHome from './components/ShopHome';
import ProductDetails from './components/ProductDetails';
import CartView from './components/CartView';
import AdminPanel from './components/AdminPanel';
import AuthView from './components/AuthView';
import WelcomeView from './components/WelcomeView';
import SearchView from './components/SearchView';
import ProfileView from './components/ProfileView';
import BottomNav from './components/BottomNav';
import { User } from '@supabase/supabase-js';

const App: React.FC = () => {
  const [currentView, setCurrentView] = useState<View>('welcome');
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [cart, setCart] = useState<CartItem[]>([]);
  const [user, setUser] = useState<User | null>(null);
  const [hasStarted, setHasStarted] = useState(false);

  useEffect(() => {
    supabase.auth.getSession().then(({ data: { session } }) => {
      const currentUser = session?.user ?? null;
      setUser(currentUser);
      if (currentUser) {
        setCurrentView('home');
        setHasStarted(true);
      }
    });

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      const currentUser = session?.user ?? null;
      setUser(currentUser);
      if (currentUser) {
        setCurrentView('home');
        setHasStarted(true);
      }
    });

    return () => subscription.unsubscribe();
  }, []);

  const addToCart = (product: Product) => {
    setCart(prev => {
      const existing = prev.find(item => item.id === product.id);
      if (existing) {
        return prev.map(item => item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item);
      }
      return [...prev, { ...product, quantity: 1 }];
    });
  };

  const updateCartQuantity = (id: string, delta: number) => {
    setCart(prev => prev.map(item => {
      if (item.id === id) {
        const newQty = Math.max(0, item.quantity + delta);
        return { ...item, quantity: newQty };
      }
      return item;
    }).filter(item => item.quantity > 0));
  };

  const navigateToDetails = (product: Product) => {
    setSelectedProduct(product);
    setCurrentView('details');
  };

  const handleStartAsGuest = () => {
    setHasStarted(true);
    setCurrentView('home');
  };

  const handleGoToAuth = () => {
    setCurrentView('auth');
  };

  const renderView = () => {
    if (currentView === 'welcome' && !user && !hasStarted) {
      return <WelcomeView onStartAsGuest={handleStartAsGuest} onGoToAuth={handleGoToAuth} />;
    }

    switch (currentView) {
      case 'home':
        return <ShopHome onProductClick={navigateToDetails} onAddToCart={addToCart} onOpenSearch={() => setCurrentView('search')} />;
      case 'search':
        return <SearchView onProductClick={navigateToDetails} onBack={() => setCurrentView('home')} onAddToCart={addToCart} />;
      case 'profile':
        return <ProfileView user={user} onBack={() => setCurrentView('home')} onAuthClick={() => setCurrentView('auth')} />;
      case 'details':
        return selectedProduct ? (
          <ProductDetails
            product={selectedProduct}
            onBack={() => setCurrentView('home')}
            onAddToCart={addToCart}
          />
        ) : <ShopHome onProductClick={navigateToDetails} onAddToCart={addToCart} onOpenSearch={() => setCurrentView('search')} />;
      case 'cart':
        return <CartView cart={cart} onUpdateQuantity={updateCartQuantity} onBack={() => setCurrentView('home')} />;
      case 'admin':
        return user ? <AdminPanel onBack={() => setCurrentView('home')} /> : <AuthView onBack={() => setCurrentView('home')} />;
      case 'auth':
        return <AuthView onBack={() => (hasStarted ? (user ? setCurrentView('profile') : setCurrentView('home')) : setCurrentView('welcome'))} />;
      default:
        return <ShopHome onProductClick={navigateToDetails} onAddToCart={addToCart} onOpenSearch={() => setCurrentView('search')} />;
    }
  };

  const showNav = ['home', 'cart', 'profile', 'admin'].includes(currentView);

  return (
    <div className="max-w-md mx-auto min-h-screen bg-[#0F0F0F] text-white shadow-xl relative overflow-hidden font-sans">
      <div className={showNav ? "pb-24" : ""}>
        {renderView()}
      </div>

      {showNav && (
        <BottomNav
          activeView={currentView}
          onNavigate={(view) => setCurrentView(view)}
          cartCount={cart.reduce((sum, item) => sum + item.quantity, 0)}
          isGuest={!user}
        />
      )}
    </div>
  );
};

export default App;
