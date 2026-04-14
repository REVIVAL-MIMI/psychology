import { useId } from "react";

type BrandLogoProps = {
  className?: string;
  size?: number;
};

export default function BrandLogo({ className, size = 38 }: BrandLogoProps) {
  const gradientId = useId();
  const glowId = useId();

  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 40 40"
      role="img"
      aria-label="Логотип ТЕЛЕКОМ БЕЗ ГРАНИЦ"
      xmlns="http://www.w3.org/2000/svg"
    >
      <defs>
        <linearGradient id={gradientId} x1="4" y1="4" x2="36" y2="36" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#11A18A" />
          <stop offset="1" stopColor="#1D6EBA" />
        </linearGradient>
        <radialGradient id={glowId} cx="0" cy="0" r="1" gradientTransform="translate(30 10) rotate(133.363) scale(21.2132)">
          <stop stopColor="#FFFFFF" stopOpacity="0.52" />
          <stop offset="1" stopColor="#FFFFFF" stopOpacity="0" />
        </radialGradient>
      </defs>

      <rect x="1" y="1" width="38" height="38" rx="12" fill={`url(#${gradientId})`} />
      <rect x="1" y="1" width="38" height="38" rx="12" fill={`url(#${glowId})`} />
      <path
        d="M10.5 19.8C10.5 14.5 14.8 10.2 20.1 10.2H29.1"
        fill="none"
        stroke="#F8FFFE"
        strokeWidth="2.4"
        strokeLinecap="round"
      />
      <path
        d="M29.5 20.2C29.5 25.5 25.2 29.8 19.9 29.8H10.9"
        fill="none"
        stroke="#F8FFFE"
        strokeWidth="2.4"
        strokeLinecap="round"
      />
      <circle cx="29.2" cy="10.4" r="2.25" fill="#F8FFFE" />
      <circle cx="10.8" cy="29.6" r="2.25" fill="#F8FFFE" />
      <circle cx="20" cy="20" r="2.1" fill="#CFFAF0" />
    </svg>
  );
}
