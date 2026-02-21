import { Link } from 'react-router-dom'
import type { Product } from '../types/database'
import ImageWithLoading from './ImageWithLoading'

interface ProductCardProps {
  product: Product
}

export default function ProductCard({ product }: ProductCardProps) {
  return (
    <Link
      to={`/product/${product.id}`}
      className="bg-card rounded-3xl overflow-hidden shadow-md hover:scale-105 transition-transform group"
    >
      <div className="relative h-48">
        <ImageWithLoading
          src={product.image_url}
          alt={product.name}
          className="w-full h-full object-cover"
          containerClassName="w-full h-full"
        />
        <div className="absolute bottom-2 right-2 bg-secondary text-black px-3 py-1 rounded-lg text-sm font-bold shadow-sm z-10">
          R$ {product.price.toFixed(2)}
        </div>
      </div>
      <div className="p-4">
        <h3 className="font-bold text-lg truncate group-hover:text-primary transition-colors">
          {product.name}
        </h3>
        <p className="text-xs text-gray-400 mt-1">
          Por: {product.sold_by || 'CompraFácil'}
        </p>
      </div>
    </Link>
  )
}
