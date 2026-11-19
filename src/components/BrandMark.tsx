import React from 'react';

interface BrandMarkProps {
  size?: number;
  className?: string;
}

export const BrandMark: React.FC<BrandMarkProps> = ({ size = 44, className = '' }) => {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 108 108"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-label="Lukuman Free Chat emblem"
    >
      <defs>
        <linearGradient id="lfc-bg" x1="0" y1="0" x2="108" y2="108" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#173166" />
          <stop offset="100%" stopColor="#0C1B3B" />
        </linearGradient>
        <linearGradient id="lfc-cobalt" x1="24" y1="24" x2="84" y2="84" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#1D4ED8" />
          <stop offset="100%" stopColor="#2563EB" />
        </linearGradient>
      </defs>
      <rect width="108" height="108" rx="30" fill="url(#lfc-bg)" />
      <path
        d="M28 28H60C71.0457 28 80 36.9543 80 48V56C80 67.0457 71.0457 76 60 76H43L29 85L33 70C29.8 66.2 28 61.3 28 56V28Z"
        fill="url(#lfc-cobalt)"
      />
      <path
        d="M39 36H68C74.6274 36 80 41.3726 80 48V53C80 59.6274 74.6274 65 68 65H48L39 73V36Z"
        fill="#38BDF8"
      />
      <path d="M46 43H52V54H65V59H46V43Z" fill="#F0F6FF" />
    </svg>
  );
};
