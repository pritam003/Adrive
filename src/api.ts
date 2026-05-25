import type { ListResponse, QuotaInfo, TrashItem, MeResponse, ShareCreateResponse, ShareInfo } from './types';

const API_BASE = '/api';

// --- Browser-side SAS cache (avoids redundant API calls) ---
interface CachedSas { url: string; exp: number }
const sasCache = new Map<string, CachedSas>();
const SAS_TTL_MS = 10 * 60 * 1000; // safe buffer below the 30-min server SAS

export function cacheReadSas(name: string, url: string) {
  sasCache.set(name, { url, exp: Date.now() + SAS_TTL_MS });
}

export async function getReadSasCached(name: string): Promise<string> {
  const c = sasCache.get(name);
  if (c && c.exp > Date.now()) return c.url;
  const url = await getReadSas(name);
  cacheReadSas(name, url);
  return url;
}

// --- Auth ---
export async function getMe(): Promise<MeResponse> {
  const res = await fetch(`${API_BASE}/me`);
  if (!res.ok) return { authenticated: false, ownerConfigured: false };
  return res.json();
}

// --- List ---
export async function listItems(prefix: string): Promise<ListResponse> {
  const res = await fetch(`${API_BASE}/list?prefix=${encodeURIComponent(prefix)}`);
  if (res.status === 401 || res.status === 403) {
    const err: any = new Error('unauthorized');
    err.status = res.status;
    throw err;
  }
  if (!res.ok) throw new Error('Failed to list');
  const data: ListResponse = await res.json();
  // Warm the SAS cache from the list response
  for (const f of data.files) {
    if (f.readSasUrl) cacheReadSas(f.name, f.readSasUrl);
  }
  return data;
}

export async function getUploadSas(name: string): Promise<string> {
  const res = await fetch(`${API_BASE}/sas?name=${encodeURIComponent(name)}&mode=upload`);
  if (!res.ok) throw new Error('Failed to get SAS');
  const data = await res.json();
  return data.url;
}

export async function getReadSas(name: string): Promise<string> {
  const res = await fetch(`${API_BASE}/sas?name=${encodeURIComponent(name)}&mode=read`);
  if (!res.ok) throw new Error('Failed to get SAS');
  const data = await res.json();
  return data.url;
}

export async function deleteFile(name: string, hard = false): Promise<void> {
  const q = `name=${encodeURIComponent(name)}${hard ? '&hard=1' : ''}`;
  const res = await fetch(`${API_BASE}/file?${q}`, { method: 'DELETE' });
  if (!res.ok) throw new Error('Failed to delete');
}

export async function renameFile(from: string, to: string): Promise<void> {
  const res = await fetch(`${API_BASE}/rename`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ from, to }),
  });
  if (!res.ok) throw new Error('Failed to rename');
}

export async function createFolder(path: string): Promise<void> {
  const res = await fetch(`${API_BASE}/folder`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ path }),
  });
  if (!res.ok) throw new Error('Failed to create folder');
}

export async function getQuota(): Promise<QuotaInfo> {
  const res = await fetch(`${API_BASE}/quota`);
  if (!res.ok) throw new Error('Failed to get quota');
  return res.json();
}

// --- Trash ---
export async function listTrash(): Promise<TrashItem[]> {
  const res = await fetch(`${API_BASE}/trash`);
  if (!res.ok) throw new Error('Failed to list trash');
  const data = await res.json();
  return data.items as TrashItem[];
}

export async function restoreFromTrash(trashKey: string): Promise<void> {
  const res = await fetch(`${API_BASE}/trash/restore`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ trashKey }),
  });
  if (!res.ok) throw new Error('Failed to restore');
}

export async function purgeTrashItem(trashKey: string): Promise<void> {
  const res = await fetch(`${API_BASE}/trash?key=${encodeURIComponent(trashKey)}`, { method: 'DELETE' });
  if (!res.ok) throw new Error('Failed to purge');
}

export async function purgeAllTrash(): Promise<void> {
  const res = await fetch(`${API_BASE}/trash?all=1`, { method: 'DELETE' });
  if (!res.ok) throw new Error('Failed to purge');
}

// --- Share ---
export async function createShare(name: string): Promise<ShareCreateResponse> {
  const res = await fetch(`${API_BASE}/share`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  });
  if (!res.ok) throw new Error('Failed to create share');
  return res.json();
}

export async function revokeShare(token: string): Promise<void> {
  const res = await fetch(`${API_BASE}/share?token=${encodeURIComponent(token)}`, { method: 'DELETE' });
  if (!res.ok) throw new Error('Failed to revoke share');
}

export async function getShareInfo(token: string): Promise<ShareInfo> {
  const res = await fetch(`${API_BASE}/share/get?token=${encodeURIComponent(token)}`);
  if (!res.ok) throw new Error('Share not found');
  return res.json();
}

// --- Upload (chunked + parallel for large files) ---
const CHUNK_THRESHOLD = 8 * 1024 * 1024; // 8 MB
const BLOCK_SIZE = 4 * 1024 * 1024; // 4 MB
const PARALLEL_BLOCKS = 4;

export async function uploadFile(
  file: File,
  blobName: string,
  onProgress?: (loaded: number, total: number) => void
): Promise<void> {
  const sasUrl = await getUploadSas(blobName);
  if (file.size < CHUNK_THRESHOLD) {
    return uploadSinglePut(file, sasUrl, onProgress);
  }
  return uploadChunked(file, sasUrl, onProgress);
}

function uploadSinglePut(
  file: File,
  sasUrl: string,
  onProgress?: (loaded: number, total: number) => void
): Promise<void> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('PUT', sasUrl);
    xhr.setRequestHeader('x-ms-blob-type', 'BlockBlob');
    xhr.setRequestHeader('x-ms-blob-content-type', file.type || 'application/octet-stream');
    xhr.setRequestHeader('x-ms-blob-cache-control', 'public, max-age=3600');
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable && onProgress) onProgress(e.loaded, e.total);
    };
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) resolve();
      else reject(new Error(`Upload failed: ${xhr.status}`));
    };
    xhr.onerror = () => reject(new Error('Upload network error'));
    xhr.send(file);
  });
}

async function uploadChunked(
  file: File,
  sasUrl: string,
  onProgress?: (loaded: number, total: number) => void
): Promise<void> {
  const numBlocks = Math.ceil(file.size / BLOCK_SIZE);
  const blockIds: string[] = [];
  const loadedPerBlock = new Array<number>(numBlocks).fill(0);

  const report = () => {
    if (!onProgress) return;
    const total = loadedPerBlock.reduce((a, b) => a + b, 0);
    onProgress(total, file.size);
  };

  for (let i = 0; i < numBlocks; i++) {
    // 64-char base64 block ID required to be identical length across blocks
    const id = btoa(String(i).padStart(16, '0'));
    blockIds.push(id);
  }

  // Run pool of PARALLEL_BLOCKS concurrent uploads
  let nextIdx = 0;
  async function worker() {
    while (true) {
      const idx = nextIdx++;
      if (idx >= numBlocks) return;
      const start = idx * BLOCK_SIZE;
      const end = Math.min(start + BLOCK_SIZE, file.size);
      const chunk = file.slice(start, end);
      const url = `${sasUrl}&comp=block&blockid=${encodeURIComponent(blockIds[idx])}`;
      await new Promise<void>((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.open('PUT', url);
        xhr.upload.onprogress = (e) => {
          if (e.lengthComputable) {
            loadedPerBlock[idx] = e.loaded;
            report();
          }
        };
        xhr.onload = () => {
          if (xhr.status >= 200 && xhr.status < 300) {
            loadedPerBlock[idx] = chunk.size;
            report();
            resolve();
          } else reject(new Error(`Block ${idx} failed: ${xhr.status}`));
        };
        xhr.onerror = () => reject(new Error(`Block ${idx} network error`));
        xhr.send(chunk);
      });
    }
  }

  await Promise.all(Array.from({ length: PARALLEL_BLOCKS }, () => worker()));

  // Commit block list
  const xml =
    '<?xml version="1.0" encoding="utf-8"?><BlockList>' +
    blockIds.map((id) => `<Latest>${id}</Latest>`).join('') +
    '</BlockList>';
  const commitUrl = `${sasUrl}&comp=blocklist`;
  const commitResp = await fetch(commitUrl, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/xml',
      'x-ms-blob-content-type': file.type || 'application/octet-stream',
      'x-ms-blob-cache-control': 'public, max-age=3600',
    },
    body: xml,
  });
  if (!commitResp.ok) throw new Error(`Commit failed: ${commitResp.status}`);
}

// --- Hover prefetch (free; warms browser HTTP cache) ---
const prefetched = new Set<string>();
export function prefetchFile(name: string, url: string, sizeBytes: number) {
  if (prefetched.has(name)) return;
  if (sizeBytes > 500 * 1024 * 1024) return; // skip very large
  prefetched.add(name);
  // Only fetch the first 256KB to warm CDN/connection without consuming bandwidth
  fetch(url, { headers: { Range: 'bytes=0-262143' }, mode: 'cors' }).catch(() => {
    prefetched.delete(name);
  });
}
