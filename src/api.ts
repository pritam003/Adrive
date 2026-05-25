import type { ListResponse, QuotaInfo } from './types';

// In dev (vite), we proxy /api -> SWA emulator if running, otherwise use prod
const API_BASE = '/api';

export async function listItems(prefix: string): Promise<ListResponse> {
  const res = await fetch(`${API_BASE}/list?prefix=${encodeURIComponent(prefix)}`);
  if (!res.ok) throw new Error('Failed to list');
  return res.json();
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

export async function deleteFile(name: string): Promise<void> {
  const res = await fetch(`${API_BASE}/file?name=${encodeURIComponent(name)}`, { method: 'DELETE' });
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

export async function uploadFile(
  file: File,
  blobName: string,
  onProgress?: (loaded: number, total: number) => void
): Promise<void> {
  const sasUrl = await getUploadSas(blobName);
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('PUT', sasUrl);
    xhr.setRequestHeader('x-ms-blob-type', 'BlockBlob');
    xhr.setRequestHeader('x-ms-blob-content-type', file.type || 'application/octet-stream');
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
