import { app, HttpRequest, HttpResponseInit, InvocationContext } from '@azure/functions';
import { randomUUID } from 'crypto';
import {
  generateUploadSas,
  generateReadSas,
  getContainerClient,
  requireOwner,
  getClientPrincipal,
  OWNER_USER_ID,
  isHiddenPath,
  TRASH_PREFIX,
  THUMB_PREFIX,
  SHARE_PREFIX,
  trashPath,
  thumbPath,
  sharePath,
} from './storage';

// ---------- helpers ----------
function thumbnailReadSas(name: string): string | null {
  // Optimistically return a SAS URL for a potential thumbnail; client falls back if 404
  return generateReadSas(thumbPath(name), 30);
}

// ---------- GET /api/me ----------
async function me(req: HttpRequest): Promise<HttpResponseInit> {
  const principal = getClientPrincipal(req);
  if (!principal) return { jsonBody: { authenticated: false, ownerConfigured: !!OWNER_USER_ID } };
  return {
    jsonBody: {
      authenticated: true,
      userId: principal.userId,
      userDetails: principal.userDetails,
      identityProvider: principal.identityProvider,
      isOwner: !OWNER_USER_ID || principal.userId === OWNER_USER_ID,
      ownerConfigured: !!OWNER_USER_ID,
    },
  };
}

// ---------- POST /api/getRoles (SWA roles function) ----------
async function getRoles(req: HttpRequest): Promise<HttpResponseInit> {
  try {
    const body = (await req.json()) as { userId?: string };
    if (!OWNER_USER_ID) return { jsonBody: { roles: ['owner'] } }; // bootstrap
    if (body.userId && body.userId === OWNER_USER_ID) return { jsonBody: { roles: ['owner'] } };
    return { jsonBody: { roles: [] } };
  } catch {
    return { jsonBody: { roles: [] } };
  }
}

// ---------- GET /api/sas?name=...&mode=upload|read ----------
async function sas(req: HttpRequest): Promise<HttpResponseInit> {
  const forbidden = requireOwner(req);
  if (forbidden) return forbidden;
  const name = req.query.get('name');
  const mode = req.query.get('mode') || 'upload';
  if (!name) return { status: 400, jsonBody: { error: 'name required' } };
  if (isHiddenPath(name)) return { status: 400, jsonBody: { error: 'invalid path' } };
  const url = mode === 'read' ? generateReadSas(name) : generateUploadSas(name);
  return { jsonBody: { url } };
}

// ---------- GET /api/list?prefix=folder/ ----------
async function list(req: HttpRequest): Promise<HttpResponseInit> {
  const forbidden = requireOwner(req);
  if (forbidden) return forbidden;
  const prefix = req.query.get('prefix') || '';
  if (isHiddenPath(prefix)) return { status: 400, jsonBody: { error: 'invalid prefix' } };
  const container = getContainerClient();
  const items: any[] = [];
  const folderSet = new Set<string>();

  for await (const item of container.listBlobsByHierarchy('/', { prefix })) {
    if (item.kind === 'prefix') {
      if (isHiddenPath(item.name)) continue;
      folderSet.add(item.name);
    } else {
      const b: any = item;
      if (isHiddenPath(b.name)) continue;
      if (b.name.endsWith('/.keep')) continue;
      const fileName = b.name;
      items.push({
        name: fileName,
        displayName: fileName.substring(prefix.length),
        size: b.properties?.contentLength || 0,
        contentType: b.properties?.contentType || 'application/octet-stream',
        lastModified: b.properties?.lastModified,
        metadata: b.metadata || {},
        // Embedded SAS — eliminates per-click roundtrip
        readSasUrl: generateReadSas(fileName, 30),
        thumbnailUrl: thumbnailReadSas(fileName),
      });
    }
  }

  return {
    jsonBody: {
      folders: Array.from(folderSet).map((f) => ({
        name: f,
        displayName: f.substring(prefix.length).replace(/\/$/, ''),
      })),
      files: items,
    },
  };
}

// ---------- DELETE /api/file?name=... (soft delete -> .trash/) ----------
async function deleteFile(req: HttpRequest): Promise<HttpResponseInit> {
  const forbidden = requireOwner(req);
  if (forbidden) return forbidden;
  const name = req.query.get('name');
  const hard = req.query.get('hard') === '1';
  if (!name) return { status: 400, jsonBody: { error: 'name required' } };
  if (isHiddenPath(name)) return { status: 400, jsonBody: { error: 'invalid path' } };

  const container = getContainerClient();
  if (hard) {
    await container.deleteBlob(name, { deleteSnapshots: 'include' });
    return { jsonBody: { deleted: name, mode: 'hard' } };
  }

  // Soft delete: copy to .trash/<timestamp>__<originalpath>, then delete original.
  const trashKey = `${Date.now()}__${name.replace(/\//g, '__')}`;
  const dst = container.getBlobClient(trashPath(trashKey));
  const src = container.getBlobClient(name);
  const poller = await dst.beginCopyFromURL(src.url);
  await poller.pollUntilDone();
  // Preserve original path in metadata
  await dst.setMetadata({ originalPath: encodeURIComponent(name), deletedAt: String(Date.now()) });
  await src.delete();

  // Best-effort: also move thumbnail
  try {
    const thumbSrc = container.getBlobClient(thumbPath(name));
    if (await thumbSrc.exists()) {
      await thumbSrc.delete();
    }
  } catch {}

  return { jsonBody: { deleted: name, mode: 'trash', trashKey } };
}

// ---------- POST /api/rename body { from, to } ----------
async function rename(req: HttpRequest): Promise<HttpResponseInit> {
  const forbidden = requireOwner(req);
  if (forbidden) return forbidden;
  const body = (await req.json()) as { from?: string; to?: string };
  if (!body.from || !body.to) return { status: 400, jsonBody: { error: 'from and to required' } };
  if (isHiddenPath(body.from) || isHiddenPath(body.to)) return { status: 400, jsonBody: { error: 'invalid path' } };
  const container = getContainerClient();
  const src = container.getBlobClient(body.from);
  const dst = container.getBlobClient(body.to);
  const poller = await dst.beginCopyFromURL(src.url);
  await poller.pollUntilDone();
  await src.delete();
  return { jsonBody: { from: body.from, to: body.to } };
}

// ---------- POST /api/folder body { path } ----------
async function createFolder(req: HttpRequest): Promise<HttpResponseInit> {
  const forbidden = requireOwner(req);
  if (forbidden) return forbidden;
  const body = (await req.json()) as { path?: string };
  if (!body.path) return { status: 400, jsonBody: { error: 'path required' } };
  if (isHiddenPath(body.path)) return { status: 400, jsonBody: { error: 'invalid path' } };
  const path = body.path.endsWith('/') ? body.path : body.path + '/';
  const marker = path + '.keep';
  const container = getContainerClient();
  await container.getBlockBlobClient(marker).upload('', 0);
  return { jsonBody: { folder: path } };
}

// ---------- GET /api/quota ----------
async function quota(req: HttpRequest): Promise<HttpResponseInit> {
  const forbidden = requireOwner(req);
  if (forbidden) return forbidden;
  const container = getContainerClient();
  let total = 0;
  let count = 0;
  let trashBytes = 0;
  let trashCount = 0;
  for await (const b of container.listBlobsFlat()) {
    if (b.name.startsWith(TRASH_PREFIX)) {
      trashBytes += b.properties.contentLength || 0;
      trashCount++;
      continue;
    }
    if (b.name.startsWith(THUMB_PREFIX) || b.name.startsWith(SHARE_PREFIX)) continue;
    if (b.name.endsWith('/.keep')) continue;
    total += b.properties.contentLength || 0;
    count++;
  }
  return { jsonBody: { totalBytes: total, fileCount: count, trashBytes, trashCount } };
}

// ---------- GET /api/trash ----------
async function listTrash(req: HttpRequest): Promise<HttpResponseInit> {
  const forbidden = requireOwner(req);
  if (forbidden) return forbidden;
  const container = getContainerClient();
  const items: any[] = [];
  for await (const b of container.listBlobsFlat({ prefix: TRASH_PREFIX, includeMetadata: true })) {
    const meta = (b as any).metadata || {};
    const original = meta.originalpath ? decodeURIComponent(meta.originalpath) : b.name.substring(TRASH_PREFIX.length);
    items.push({
      trashKey: b.name.substring(TRASH_PREFIX.length),
      originalPath: original,
      size: b.properties.contentLength || 0,
      contentType: b.properties.contentType || 'application/octet-stream',
      deletedAt: meta.deletedat ? Number(meta.deletedat) : null,
      readSasUrl: generateReadSas(b.name, 30),
    });
  }
  return { jsonBody: { items } };
}

// ---------- POST /api/trash/restore body { trashKey } ----------
async function restoreFromTrash(req: HttpRequest): Promise<HttpResponseInit> {
  const forbidden = requireOwner(req);
  if (forbidden) return forbidden;
  const body = (await req.json()) as { trashKey?: string };
  if (!body.trashKey) return { status: 400, jsonBody: { error: 'trashKey required' } };
  const container = getContainerClient();
  const src = container.getBlobClient(TRASH_PREFIX + body.trashKey);
  const props = await src.getProperties();
  const original = props.metadata?.originalpath
    ? decodeURIComponent(props.metadata.originalpath)
    : body.trashKey.replace(/^\d+__/, '').replace(/__/g, '/');
  const dst = container.getBlobClient(original);
  const poller = await dst.beginCopyFromURL(src.url);
  await poller.pollUntilDone();
  await src.delete();
  return { jsonBody: { restored: original } };
}

// ---------- DELETE /api/trash?key=... (purge single) or all=1 ----------
async function purgeTrash(req: HttpRequest): Promise<HttpResponseInit> {
  const forbidden = requireOwner(req);
  if (forbidden) return forbidden;
  const container = getContainerClient();
  const all = req.query.get('all') === '1';
  if (all) {
    let count = 0;
    for await (const b of container.listBlobsFlat({ prefix: TRASH_PREFIX })) {
      await container.deleteBlob(b.name);
      count++;
    }
    return { jsonBody: { purged: count } };
  }
  const key = req.query.get('key');
  if (!key) return { status: 400, jsonBody: { error: 'key required' } };
  await container.deleteBlob(TRASH_PREFIX + key);
  return { jsonBody: { purged: 1 } };
}

// ---------- POST /api/share body { name } -> create share token ----------
async function createShare(req: HttpRequest): Promise<HttpResponseInit> {
  const forbidden = requireOwner(req);
  if (forbidden) return forbidden;
  const body = (await req.json()) as { name?: string };
  if (!body.name) return { status: 400, jsonBody: { error: 'name required' } };
  if (isHiddenPath(body.name)) return { status: 400, jsonBody: { error: 'invalid path' } };

  const container = getContainerClient();
  const src = container.getBlobClient(body.name);
  const exists = await src.exists();
  if (!exists) return { status: 404, jsonBody: { error: 'file not found' } };

  const token = randomUUID().replace(/-/g, '');
  const record = {
    token,
    name: body.name,
    createdAt: Date.now(),
  };
  const data = Buffer.from(JSON.stringify(record), 'utf-8');
  await container.getBlockBlobClient(sharePath(token)).upload(data, data.length, {
    blobHTTPHeaders: { blobContentType: 'application/json' },
  });
  return { jsonBody: { token, name: body.name } };
}

// ---------- DELETE /api/share?token=... ----------
async function revokeShare(req: HttpRequest): Promise<HttpResponseInit> {
  const forbidden = requireOwner(req);
  if (forbidden) return forbidden;
  const token = req.query.get('token');
  if (!token) return { status: 400, jsonBody: { error: 'token required' } };
  const container = getContainerClient();
  try {
    await container.deleteBlob(sharePath(token));
  } catch {}
  return { jsonBody: { revoked: token } };
}

// ---------- GET /api/share/get?token=... (PUBLIC — viewer endpoint) ----------
async function getShare(req: HttpRequest): Promise<HttpResponseInit> {
  const token = req.query.get('token');
  if (!token || !/^[a-f0-9]{32}$/i.test(token)) return { status: 400, jsonBody: { error: 'invalid token' } };
  const container = getContainerClient();
  const recordBlob = container.getBlobClient(sharePath(token));
  if (!(await recordBlob.exists())) return { status: 404, jsonBody: { error: 'not found' } };
  const buf = await recordBlob.downloadToBuffer();
  const record = JSON.parse(buf.toString('utf-8')) as { name: string };

  const file = container.getBlobClient(record.name);
  if (!(await file.exists())) return { status: 404, jsonBody: { error: 'file removed' } };
  const props = await file.getProperties();

  const parts = record.name.split('/');
  return {
    jsonBody: {
      token,
      displayName: parts[parts.length - 1],
      size: props.contentLength || 0,
      contentType: props.contentType || 'application/octet-stream',
      sasUrl: generateReadSas(record.name, 60),
      thumbnailUrl: generateReadSas(thumbPath(record.name), 60),
    },
  };
}

// ---------- POST /api/thumb body { name } (server-side fallback; client preferred) ----------
// Currently a stub: clients generate thumbnails and upload directly. This endpoint
// records that a thumbnail upload SAS is wanted.
async function thumbSas(req: HttpRequest): Promise<HttpResponseInit> {
  const forbidden = requireOwner(req);
  if (forbidden) return forbidden;
  const body = (await req.json()) as { name?: string };
  if (!body.name) return { status: 400, jsonBody: { error: 'name required' } };
  if (isHiddenPath(body.name)) return { status: 400, jsonBody: { error: 'invalid path' } };
  const url = generateUploadSas(thumbPath(body.name), 30);
  return { jsonBody: { url } };
}

// ---------- Register routes ----------
app.http('me', { route: 'me', methods: ['GET'], authLevel: 'anonymous', handler: me });
app.http('getRoles', { route: 'getRoles', methods: ['POST'], authLevel: 'anonymous', handler: getRoles });
app.http('sas', { route: 'sas', methods: ['GET'], authLevel: 'anonymous', handler: sas });
app.http('list', { route: 'list', methods: ['GET'], authLevel: 'anonymous', handler: list });
app.http('file', { route: 'file', methods: ['DELETE'], authLevel: 'anonymous', handler: deleteFile });
app.http('rename', { route: 'rename', methods: ['POST'], authLevel: 'anonymous', handler: rename });
app.http('folder', { route: 'folder', methods: ['POST'], authLevel: 'anonymous', handler: createFolder });
app.http('quota', { route: 'quota', methods: ['GET'], authLevel: 'anonymous', handler: quota });

app.http('listTrash', { route: 'trash', methods: ['GET'], authLevel: 'anonymous', handler: listTrash });
app.http('restoreFromTrash', { route: 'trash/restore', methods: ['POST'], authLevel: 'anonymous', handler: restoreFromTrash });
app.http('purgeTrash', { route: 'trash', methods: ['DELETE'], authLevel: 'anonymous', handler: purgeTrash });

app.http('createShare', { route: 'share', methods: ['POST'], authLevel: 'anonymous', handler: createShare });
app.http('revokeShare', { route: 'share', methods: ['DELETE'], authLevel: 'anonymous', handler: revokeShare });
app.http('getShare', { route: 'share/get', methods: ['GET'], authLevel: 'anonymous', handler: getShare });

app.http('thumbSas', { route: 'thumb', methods: ['POST'], authLevel: 'anonymous', handler: thumbSas });
