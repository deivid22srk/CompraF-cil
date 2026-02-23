import { Link } from 'react-router-dom'
import type { Product } from '../types/database'
import ImageWithLoading from './ImageWithLoading'

interface ProductCardProps {
  product: Product
}

export default function ProductCard({ product }: ProductCardProps) {
  const isLowStock = product.stock_quantity && product.stock_quantity > 0 && product.stock_quantity <= 5;

  return (
    <Link
      to={`/product/${product.id}`}
      className="bg-card rounded-[2rem] overflow-hidden border border-white/5 hover:border-primary/30 transition-all group flex flex-col"
    >
      <div className="relative h-44 p-2">
        <ImageWithLoading
          src={product.image_url}
          alt={product.name}
          className="w-full h-full object-cover rounded-3xl"
          containerClassName="w-full h-full"
        />
        {isLowStock && (
          <div className="absolute top-4 left-4 bg-red-600 text-white text-[10px] font-black px-2 py-1 rounded-md shadow-lg z-10">
            ÚLTIMAS UNIDADES
          </div>
        )}
      </div>
      <div className="p-4 pt-2 flex flex-col flex-1">
        <h3 className="font-black text-base truncate group-hover:text-primary transition-colors">
          {product.name}
        </h3>
        <p className="text-[10px] text-gray-500 uppercase font-bold tracking-wider">
          {product.sold_by || 'CompraFácil'}
        </p>

        <div className="mt-auto pt-4 flex items-center justify-between">
          <span className="text-primary font-black text-xl">
            R$ {product.price.toFixed(2)}
          </span>
          <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center text-primary group-hover:bg-primary group-hover:text-black transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
          </div>
        </div>
      </div>
    </Link>
  )
}
