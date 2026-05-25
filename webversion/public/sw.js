/* Adrive background upload service worker.
 * Owns the upload lifecycle so transfers survive page reloads.
 *
 * Messages from page → SW:
 *   { type: 'upload', id, file, sasUrl, blobName, name, contentType }
 *   { type: 'list' }   → SW broadcasts { type: 'list', items: [...] }
 *   { type: 'clear' }  → drops done/error entries
 *
 * Messages from SW → page (broadcast to all clients):
 *   { type: 'progress', id, name, blobName, loaded, total, status }
 *   { type: 'done',     id, name, blobName }
 *   { type: 'error',    id, name, blobName, error }
 *   { type: 'list',     items: [...] }
 */

const CHUNK_THRESHOLD = 8 * 1024 * 1024; // 8 MB
const BLOCK_SIZE = 4 * 1024 * 1024;       // 4 MB
const PARALLEL_BLOCKS = 4;
const MAX_CONCURRENT_FILES = 2;

const uploads = new Map(); // id -> state
const queue = [];
let active = 0;

self.addEventListener('install', (e) => {
  self.skipWaiting();
});

self.addEventListener('activate', (e) => {
  e.waitUntil(self.clients.claim());
});

async function broadcast(msg) {
  const clients = await self.clients.matchAll({ includeUncontrolled: true, type: 'window' });
  for (const c of clients) c.postMessage(msg);
}

function snapshot(state) {
  return {
    id: state.id,
    name: state.name,
    blobName: state.blobName,
    loaded: state.loaded,
    total: state.total,
    status: state.status,
    error: state.error,
  };
}

async function putSingle(state) {
  const { file, sasUrl, contentType } = state;
  const res = await fetch(sasUrl, {
    method: 'PUT',
    headers: {
      'x-ms-blob-type': 'BlockBlob',
      'x-ms-blob-content-type': contentType || 'application/octet-stream',
      'x-ms-blob-cache-control': 'public, max-age=3600',
    },
    body: file,
  });
  if (!res.ok) throw new Error('PUT ' + res.status);
  state.loaded = file.size;
  broadcast({ type: 'progress', ...snapshot(state) });
}

async function putBlock(sasUrl, blockId, chunk) {
  const url = sasUrl + '&comp=block&blockid=' + encodeURIComponent(blockId);
  const res = await fetch(url, { method: 'PUT', body: chunk });
  if (!res.ok) throw new Error('block PUT ' + res.status);
}

async function commitBlockList(state, blockIds) {
  const { sasUrl, contentType } = state;
  const url = sasUrl + '&comp=blocklist';
  const xml =
    '<?xml version="1.0" encoding="utf-8"?><BlockList>' +
    blockIds.map((id) => '<Latest>' + id + '</Latest>').join('') +
    '</BlockList>';
  const res = await fetch(url, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/xml',
      'x-ms-blob-content-type': contentType || 'application/octet-stream',
      'x-ms-blob-cache-control': 'public, max-age=3600',
    },
    body: xml,
  });
  if (!res.ok) throw new Error('commit ' + res.status);
}

async function putChunked(state) {
  const { file, sasUrl } = state;
  const numBlocks = Math.ceil(file.size / BLOCK_SIZE);
  const blockIds = [];
  for (let i = 0; i < numBlocks; i++) {
    blockIds.push(btoa(String(i).padStart(10, '0')));
  }

  let next = 0;
  const workers = Array.from({ length: PARALLEL_BLOCKS }, async () => {
    while (true) {
      const i = next++;
      if (i >= numBlocks) return;
      const start = i * BLOCK_SIZE;
      const end = Math.min(start + BLOCK_SIZE, file.size);
      const chunk = file.slice(start, end);
      await putBlock(sasUrl, blockIds[i], chunk);
      state.loaded += end - start;
      broadcast({ type: 'progress', ...snapshot(state) });
    }
  });
  await Promise.all(workers);
  await commitBlockList(state, blockIds);
}

async function runUpload(state) {
  state.status = 'uploading';
  broadcast({ type: 'progress', ...snapshot(state) });
  try {
    if (state.file.size < CHUNK_THRESHOLD) {
      await putSingle(state);
    } else {
      await putChunked(state);
    }
    state.status = 'done';
    state.loaded = state.total;
    broadcast({ type: 'done', ...snapshot(state) });
  } catch (e) {
    state.status = 'error';
    state.error = String((e && e.message) || e);
    broadcast({ type: 'error', ...snapshot(state) });
  } finally {
    // Release file reference so memory can be reclaimed once consumers
    // have read the final status (keep meta around for late listeners).
    state.file = null;
  }
}

function pump() {
  while (active < MAX_CONCURRENT_FILES && queue.length > 0) {
    const state = queue.shift();
    active++;
    runUpload(state).finally(() => {
      active--;
      pump();
    });
  }
}

self.addEventListener('message', (ev) => {
  const data = ev.data;
  if (!data || typeof data !== 'object') return;

  if (data.type === 'upload') {
    const { id, file, sasUrl, blobName, name, contentType } = data;
    if (uploads.has(id)) return;
    const state = {
      id,
      file,
      sasUrl,
      blobName,
      name,
      contentType,
      loaded: 0,
      total: file.size,
      status: 'queued',
      error: null,
    };
    uploads.set(id, state);
    queue.push(state);
    broadcast({ type: 'progress', ...snapshot(state) });
    pump();
  } else if (data.type === 'list') {
    const items = Array.from(uploads.values()).map(snapshot);
    broadcast({ type: 'list', items });
  } else if (data.type === 'clear') {
    for (const [k, v] of uploads) if (v.status !== 'uploading' && v.status !== 'queued') uploads.delete(k);
  }
});
