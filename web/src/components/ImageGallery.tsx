interface ImageGalleryProps {
  images: { id: string; image_url: string }[]
  selectedImage: string
  onSelect: (url: string) => void
}

import ImageWithLoading from './ImageWithLoading'

export default function ImageGallery({ images, selectedImage, onSelect }: ImageGalleryProps) {
  if (images.length <= 1) return null

  return (
    <div className="p-4 flex gap-2 overflow-x-auto bg-surface/50 scrollbar-hide">
      {images.map((img) => (
        <button
          key={img.id}
          onClick={() => onSelect(img.image_url)}
          className={`relative flex-shrink-0 w-20 h-20 rounded-xl overflow-hidden border-2 transition-all ${
            selectedImage === img.image_url ? 'border-primary' : 'border-transparent opacity-60 hover:opacity-100'
          }`}
        >
          <ImageWithLoading
            src={img.image_url}
            className="w-full h-full object-cover"
            containerClassName="w-full h-full"
            alt=""
          />
        </button>
      ))}
    </div>
  )
}
