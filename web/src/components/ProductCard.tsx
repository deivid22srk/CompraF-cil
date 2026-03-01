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
      className="bg-card rounded-[1.5rem] overflow-hidden border border-white/5 hover:border-primary/30 transition-all group flex flex-col h-full"
    >
      <div className="relative aspect-square p-1.5">
        <ImageWithLoading
          src={product.image_url}
          alt={product.name}
          className="w-full h-full object-cover rounded-[1.2rem]"
          containerClassName="w-full h-full"
        />
        {isLowStock && (
          <div className="absolute top-3 left-3 bg-red-600 text-white text-[8px] font-black px-1.5 py-0.5 rounded shadow-lg z-10 uppercase">
            Últimas unidades
          </div>
        )}
      </div>
      <div className="p-3 pt-1 flex flex-col flex-1">
        <p className="text-[8px] text-gray-500 uppercase font-black tracking-widest mb-1">
          {product.sold_by || 'CompraFácil'}
        </p>
        <h3 className="font-black text-xs md:text-sm line-clamp-2 leading-tight group-hover:text-primary transition-colors flex-1">
          {product.name}
        </h3>

        <div className="mt-3 flex items-center justify-between gap-2">
          <span className="text-primary font-black text-base whitespace-nowrap">
            R$ {product.price.toFixed(2)}
          </span>
          <div className="w-7 h-7 rounded-xl bg-primary/10 flex items-center justify-center text-primary group-hover:bg-primary group-hover:text-black transition-colors shrink-0">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
          </div>
        </div>
      </div>
    </Link>
  )
}
