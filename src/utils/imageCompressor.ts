const MAX_DIMENSION_PX = 800;
const MAX_JPEG_BYTES = 150 * 1024; // 150 KB

function loadImageElement(file: File): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      URL.revokeObjectURL(objectUrl);
      resolve(img);
    };
    img.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      reject(new Error('Could not decode image file.'));
    };
    img.src = objectUrl;
  });
}

function approximateByteSize(base64: string): number {
  const padding = base64.endsWith('==') ? 2 : base64.endsWith('=') ? 1 : 0;
  return Math.floor((base64.length * 3) / 4) - padding;
}

export async function compressImageFileToBase64(file: File): Promise<string> {
  const img = await loadImageElement(file);
  const srcWidth = img.naturalWidth || img.width;
  const srcHeight = img.naturalHeight || img.height;
  if (!srcWidth || !srcHeight) {
    throw new Error('Invalid image dimensions.');
  }

  const longest = Math.max(srcWidth, srcHeight);
  const initialScale = longest > MAX_DIMENSION_PX ? MAX_DIMENSION_PX / longest : 1;
  let targetWidth = Math.max(1, Math.round(srcWidth * initialScale));
  let targetHeight = Math.max(1, Math.round(srcHeight * initialScale));

  const canvas = document.createElement('canvas');
  const ctx = canvas.getContext('2d');
  if (!ctx) {
    throw new Error('Canvas 2D context unavailable.');
  }

  for (let scaleStep = 0; scaleStep <= 3; scaleStep++) {
    canvas.width = targetWidth;
    canvas.height = targetHeight;
    ctx.clearRect(0, 0, targetWidth, targetHeight);
    ctx.drawImage(img, 0, 0, targetWidth, targetHeight);

    for (let quality = 0.85; quality >= 0.25; quality -= quality > 0.45 ? 0.1 : 0.05) {
      const dataUrl = canvas.toDataURL('image/jpeg', Number(quality.toFixed(2)));
      const base64 = dataUrl.split(',')[1] || '';
      if (base64 && approximateByteSize(base64) < MAX_JPEG_BYTES) {
        return base64;
      }
    }

    targetWidth = Math.max(160, Math.round(targetWidth * 0.8));
    targetHeight = Math.max(160, Math.round(targetHeight * 0.8));
  }

  throw new Error('Compressed image exceeds 150 KB limit.');
}
