import React, { useState } from 'react';

interface UserAvatarProps {
  name: string;
  photoUrl?: string;
  sizeClass?: string;
}

export const UserAvatar: React.FC<UserAvatarProps> = ({
  name,
  photoUrl = '',
  sizeClass = 'w-11 h-11 text-sm',
}) => {
  const [imgFailed, setImgFailed] = useState(false);

  const initials =
    name
      .trim()
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? '')
      .join('') || 'L';

  return (
    <div
      className={`${sizeClass} rounded-full shrink-0 overflow-hidden bg-gradient-to-br from-[#1D4ED8] to-[#173166] border border-[#213C73] flex items-center justify-center font-semibold text-[#F0F6FF] select-none`}
    >
      {photoUrl && !imgFailed ? (
        <img
          src={photoUrl}
          alt={name}
          referrerPolicy="no-referrer"
          onError={() => setImgFailed(true)}
          className="w-full h-full object-cover"
        />
      ) : (
        <span>{initials}</span>
      )}
    </div>
  );
};
