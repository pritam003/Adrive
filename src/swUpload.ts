// Service-worker upload bridge.
// Uploads are owned by the SW, so they keep running across page reloads.

export interface SWUploadItem {
  id: string;
  name: string;
  blobName: string;
  loaded: number;
  total: number;
  status: 'queued' | 'uploading' | 'done' | 'error';
  error?: string | null;
}

type Msg =
  | ({ type: 'progress' } & SWUploadItem)
  | ({ type: 'done' } & SWUploadItem)
  | ({ type: 'error' } & SWUploadItem)
  | { type: 'list'; items: SWUploadItem[] };

type Listener = (msg: Msg) => void;
const listeners = new Set<Listener>();
let regPromise: Promise<ServiceWorkerRegistration> | null = null;

export function initUploadWorker() {
  if (typeof navigator === 'undefined' || !('serviceWorker' in navigator)) return;
  if (regPromise) return;
  regPromise = navigator.serviceWorker
    .register('/sw.js', { scope: '/' })
    .then(async (reg) => {
      // Make sure we have an active worker before anyone tries to post to it.
      if (!reg.active) {
        await new Promise<void>((resolve) => {
          const sw = reg.installing || reg.waiting;
          if (!sw) return resolve();
          sw.addEventListener('statechange', () => {
            if (sw.state === 'activated') resolve();
          });
        });
      }
      return reg;
    });
  navigator.serviceWorker.addEventListener('message', (ev) => {
    const data = ev.data as Msg | undefined;
    if (!data || typeof data !== 'object') return;
    listeners.forEach((l) => l(data));
  });
}

export function subscribeUploads(fn: Listener): () => void {
  listeners.add(fn);
  return () => {
    listeners.delete(fn);
  };
}

async function getWorker(): Promise<ServiceWorker | null> {
  if (!('serviceWorker' in navigator)) return null;
  if (!regPromise) initUploadWorker();
  const reg = await regPromise!;
  return reg.active || reg.waiting || reg.installing || null;
}

export async function requestUploadList(): Promise<void> {
  const sw = await getWorker();
  sw?.postMessage({ type: 'list' });
}

export async function clearFinishedUploads(): Promise<void> {
  const sw = await getWorker();
  sw?.postMessage({ type: 'clear' });
}

export async function enqueueUpload(opts: {
  id: string;
  file: File;
  sasUrl: string;
  blobName: string;
  name: string;
}): Promise<void> {
  const sw = await getWorker();
  if (!sw) throw new Error('Service worker unavailable; uploads cannot run in background.');
  sw.postMessage({
    type: 'upload',
    id: opts.id,
    file: opts.file,
    sasUrl: opts.sasUrl,
    blobName: opts.blobName,
    name: opts.name,
    contentType: opts.file.type || 'application/octet-stream',
  });
}
