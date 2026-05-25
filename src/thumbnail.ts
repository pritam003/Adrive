// Client-side thumbnail generation. Produces a JPEG <= 320px on the longer edge.

const MAX_DIM = 320;
const QUALITY = 0.72;

export async function generateThumbnail(file: File): Promise<Blob | null> {
  if (file.type.startsWith('image/')) return imageThumb(file);
  if (file.type.startsWith('video/')) return videoThumb(file);
  return null;
}

async function imageThumb(file: File): Promise<Blob | null> {
  const url = URL.createObjectURL(file);
  try {
    const img = await loadImage(url);
    return drawToBlob(img, img.naturalWidth, img.naturalHeight);
  } finally {
    URL.revokeObjectURL(url);
  }
}

async function videoThumb(file: File): Promise<Blob | null> {
  const url = URL.createObjectURL(file);
  try {
    const video = document.createElement('video');
    video.src = url;
    video.muted = true;
    video.playsInline = true;
    video.preload = 'metadata';
    await new Promise<void>((resolve, reject) => {
      video.onloadedmetadata = () => {
        // Seek 1 second in (or 10% of duration)
        video.currentTime = Math.min(1, (video.duration || 1) * 0.1);
      };
      video.onseeked = () => resolve();
      video.onerror = () => reject(new Error('video load failed'));
    });
    return drawToBlob(video, video.videoWidth, video.videoHeight);
  } catch {
    return null;
  } finally {
    URL.revokeObjectURL(url);
  }
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error('image load failed'));
    img.src = src;
  });
}

function drawToBlob(
  source: CanvasImageSource,
  w: number,
  h: number
): Promise<Blob | null> {
  if (!w || !h) return Promise.resolve(null);
  const scale = Math.min(1, MAX_DIM / Math.max(w, h));
  const tw = Math.round(w * scale);
  const th = Math.round(h * scale);
  const canvas = document.createElement('canvas');
  canvas.width = tw;
  canvas.height = th;
  const ctx = canvas.getContext('2d');
  if (!ctx) return Promise.resolve(null);
  ctx.drawImage(source, 0, 0, tw, th);
  return new Promise((resolve) => canvas.toBlob((b) => resolve(b), 'image/jpeg', QUALITY));
}

export async function uploadThumbnail(blob: Blob, fileName: string): Promise<void> {
  // Get a SAS for the thumbnail path
  const res = await fetch('/api/thumb', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: fileName }),
  });
  if (!res.ok) return;
  const { url } = await res.json();
  await fetch(url, {
    method: 'PUT',
    headers: {
      'x-ms-blob-type': 'BlockBlob',
      'x-ms-blob-content-type': 'image/jpeg',
      'x-ms-blob-cache-control': 'public, max-age=86400',
    },
    body: blob,
  });
}