import { app, HttpRequest, HttpResponseInit, InvocationContext } from '@azure/functions';
import { generateUploadSas, generateReadSas, getContainerClient } from './storage';

// GET /api/sas?name=path/to/file.ext  -> returns upload SAS url
async function sas(req: HttpRequest, _ctx: InvocationContext): Promise<HttpResponseInit> {
  const name = req.query.get('name');
  const mode = req.query.get('mode') || 'upload';
  if (!name) return { status: 400, jsonBody: { error: 'name required' } };
  const url = mode === 'read' ? generateReadSas(name) : generateUploadSas(name);
  return { jsonBody: { url } };
}

// GET /api/list?prefix=folder/
async function list(req: HttpRequest, _ctx: InvocationContext): Promise<HttpResponseInit> {
  const prefix = req.query.get('prefix') || '';
  const container = getContainerClient();
  const items: any[] = [];
  const folderSet = new Set<string>();

  for await (const item of container.listBlobsByHierarchy('/', { prefix })) {
    if (item.kind === 'prefix') {
      folderSet.add(item.name);
    } else {
      const b: any = item;
      items.push({
        name: b.name,
        displayName: b.name.substring(prefix.length),
        size: b.properties?.contentLength || 0,
        contentType: b.properties?.contentType || 'application/octet-stream',
        lastModified: b.properties?.lastModified,
        metadata: b.metadata || {},
      });
    }
  }

  return {
    jsonBody: {
      folders: Array.from(folderSet).map((f) => ({ name: f, displayName: f.substring(prefix.length).replace(/\/$/, '') })),
      files: items,
    },
  };
}

// DELETE /api/file?name=...
async function deleteFile(req: HttpRequest, _ctx: InvocationContext): Promise<HttpResponseInit> {
  const name = req.query.get('name');
  if (!name) return { status: 400, jsonBody: { error: 'name required' } };
  const container = getContainerClient();
  await container.deleteBlob(name, { deleteSnapshots: 'include' });
  return { jsonBody: { deleted: name } };
}

// POST /api/rename  body: { from, to }
async function rename(req: HttpRequest, _ctx: InvocationContext): Promise<HttpResponseInit> {
  const body = (await req.json()) as { from?: string; to?: string };
  if (!body.from || !body.to) return { status: 400, jsonBody: { error: 'from and to required' } };
  const container = getContainerClient();
  const src = container.getBlobClient(body.from);
  const dst = container.getBlobClient(body.to);
  const poller = await dst.beginCopyFromURL(src.url);
  await poller.pollUntilDone();
  await src.delete();
  return { jsonBody: { from: body.from, to: body.to } };
}

// POST /api/folder  body: { path } -> creates a 0-byte marker blob to make empty folder visible
async function createFolder(req: HttpRequest, _ctx: InvocationContext): Promise<HttpResponseInit> {
  const body = (await req.json()) as { path?: string };
  if (!body.path) return { status: 400, jsonBody: { error: 'path required' } };
  const path = body.path.endsWith('/') ? body.path : body.path + '/';
  const marker = path + '.keep';
  const container = getContainerClient();
  await container.getBlockBlobClient(marker).upload('', 0);
  return { jsonBody: { folder: path } };
}

// GET /api/quota
async function quota(_req: HttpRequest, _ctx: InvocationContext): Promise<HttpResponseInit> {
  const container = getContainerClient();
  let total = 0;
  let count = 0;
  for await (const b of container.listBlobsFlat()) {
    total += b.properties.contentLength || 0;
    count++;
  }
  return { jsonBody: { totalBytes: total, fileCount: count } };
}

// Register routes
app.http('sas', { route: 'sas', methods: ['GET'], authLevel: 'anonymous', handler: sas });
app.http('list', { route: 'list', methods: ['GET'], authLevel: 'anonymous', handler: list });
app.http('file', { route: 'file', methods: ['DELETE'], authLevel: 'anonymous', handler: deleteFile });
app.http('rename', { route: 'rename', methods: ['POST'], authLevel: 'anonymous', handler: rename });
app.http('folder', { route: 'folder', methods: ['POST'], authLevel: 'anonymous', handler: createFolder });
app.http('quota', { route: 'quota', methods: ['GET'], authLevel: 'anonymous', handler: quota });
