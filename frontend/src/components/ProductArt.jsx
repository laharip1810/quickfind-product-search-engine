import { useState } from 'react';

/**
 * Generated product illustration: a line icon for the subcategory on a background
 * tinted with the product's first color. Real image URLs (imageUrl) take precedence.
 */
const ICONS = {
  shoe: (
    <>
      <path d="M10 60c0-8 8-11 18-13l14-8c4-2 8-1 10 3l4 7c8 4 22 6 30 10 4 2 4 10 0 11H14c-3 0-4-3-4-10z" />
      <path d="M10 64h80M40 44l4 6M46 41l4 6" />
    </>
  ),
  tshirt: <path d="M36 20L20 28l-8 16 12 6 4-6v38h44V44l4 6 12-6-8-16-16-8c-3 6-8 10-14 10s-11-4-14-10z" />,
  pants: (
    <>
      <path d="M30 16h40l4 70H56l-6-44-6 44H26z" />
      <path d="M30 24h40" />
    </>
  ),
  shorts: (
    <>
      <path d="M28 26h44l6 38-22 2-6-22-6 22-22-2z" />
      <path d="M28 33h44" />
    </>
  ),
  jacket: (
    <>
      <path d="M36 18l-16 8-6 54h12l2-34v38h44V46l2 34h12l-6-54-16-8-14 12z" />
      <path d="M50 30v54M42 58h4M54 58h4" />
    </>
  ),
  watch: (
    <>
      <circle cx="50" cy="50" r="18" />
      <path d="M40 34l2-20h16l2 20M40 66l2 20h16l2-20M50 50V40M50 50l8 4" />
    </>
  ),
  bag: (
    <>
      <path d="M28 34c0-10 8-14 22-14s22 4 22 14l2 50H26z" />
      <path d="M36 56h28v20H36zM42 20c0-8 16-8 16 0" />
    </>
  ),
  gear: <path d="M18 40v20M26 32v36M74 32v36M82 40v20M26 50h48" />,
};

const ICON_BY_SUBCATEGORY = {
  'Running Shoes': 'shoe',
  Sneakers: 'shoe',
  'T-Shirts': 'tshirt',
  Jeans: 'pants',
  'Track Pants': 'pants',
  Shorts: 'shorts',
  Jackets: 'jacket',
  Watches: 'watch',
  Bags: 'bag',
  'Sports Accessories': 'gear',
};

function tint(hex, amount) {
  const value = parseInt((hex || '#9CA3AF').slice(1), 16);
  const mix = (c) => Math.round(c + (255 - c) * amount);
  const r = mix((value >> 16) & 255);
  const g = mix((value >> 8) & 255);
  const b = mix(value & 255);
  return `rgb(${r}, ${g}, ${b})`;
}

export default function ProductArt({ product, size = 'card' }) {
  const [imageFailed, setImageFailed] = useState(false);
  if (product.imageUrl && !imageFailed) {
    return (
      <div className={`product-art product-art-${size}`}>
        <img src={product.imageUrl} alt={product.name} loading="lazy" onError={() => setImageFailed(true)} />
      </div>
    );
  }
  const hex = product.colors?.[0]?.hexCode;
  const icon = ICONS[ICON_BY_SUBCATEGORY[product.subcategory] || 'gear'];
  return (
    <div
      className={`product-art product-art-${size}`}
      style={{ background: `linear-gradient(135deg, ${tint(hex, 0.82)} 0%, ${tint(hex, 0.93)} 100%)` }}
      role="img"
      aria-label={`${product.subcategory} illustration`}
    >
      <svg viewBox="0 0 100 100" aria-hidden="true">
        <g fill={tint(hex, 0.55)} stroke="#1e293b" strokeWidth="2.2" strokeLinejoin="round" strokeLinecap="round">
          {icon}
        </g>
      </svg>
      <span className="product-art-brand">{product.brand}</span>
    </div>
  );
}
