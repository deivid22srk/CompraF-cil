import { useState } from 'react'

interface ImageWithLoadingProps extends React.ImgHTMLAttributes<HTMLImageElement> {
  containerClassName?: string
}

export default function ImageWithLoading({
  src,
  alt,
  className,
  containerClassName,
  ...props
}: ImageWithLoadingProps) {
  const [isLoading, setIsLoading] = useState(true)

  return (
    <div className={`relative overflow-hidden ${containerClassName || ''}`}>
      {isLoading && (
        <div className="absolute inset-0 bg-white/5 animate-pulse flex items-center justify-center">
          <div className="w-8 h-8 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></div>
        </div>
      )}
      <img
        src={src}
        alt={alt}
        className={`${className || ''} ${isLoading ? 'opacity-0' : 'opacity-100'} transition-opacity duration-300`}
        onLoad={() => setIsLoading(false)}
        {...props}
      />
    </div>
  )
}
