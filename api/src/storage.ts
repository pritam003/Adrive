import { BlobServiceClient, StorageSharedKeyCredential, generateBlobSASQueryParameters, BlobSASPermissions } from '@azure/storage-blob';
import { HttpRequest, HttpResponseInit } from '@azure/functions';

const CONN = process.env.AZURE_STORAGE_CONNECTION_STRING || '';
const CONTAINER = process.env.STORAGE_CONTAINER || 'drive-files';
const OWNER_USER_ID = process.env.OWNER_USER_ID || '';

export const TRASH_PREFIX = '.trash/';
export const THUMB_PREFIX = '.thumbnails/';
export const SHARE_PREFIX = '.shares/';

function parseConn(): { accountName: string; accountKey: string } {
  const parts: Record<string, string> = {};
  CONN.split(';').forEach((p) => {
    const idx = p.indexOf('=');
    if (idx > 0) parts[p.substring(0, idx)] = p.substring(idx + 1);
  });
  return { accountName: parts['AccountName'], accountKey: parts['AccountKey'] };
}

function encodeBlobPath(name: string): string {
  return name.split('/').map(encodeURIComponent).join('/');
}

export function getBlobService(): BlobServiceClient {
  return BlobServiceClient.fromConnectionString(CONN);
}

export function getContainerClient() {
  return getBlobService().getContainerClient(CONTAINER);
}

export const containerName = CONTAINER;
export { OWNER_USER_ID };

export function generateUploadSas(blobName: string, expiryMinutes = 60): string {
  const { accountName, accountKey } = parseConn();
  const creds = new StorageSharedKeyCredential(accountName, accountKey);
  const expires = new Date(Date.now() + expiryMinutes * 60 * 1000);
  const sas = generateBlobSASQueryParameters(
    {
      containerName: CONTAINER,
      blobName,
      permissions: BlobSASPermissions.parse('racwd'),
      expiresOn: expires,
    },
    creds
  ).toString();
  return `https://${accountName}.blob.core.windows.net/${CONTAINER}/${encodeBlobPath(blobName)}?${sas}`;
}

export function generateReadSas(blobName: string, expiryMinutes = 30): string {
  const { accountName, accountKey } = parseConn();
  const creds = new StorageSharedKeyCredential(accountName, accountKey);
  const expires = new Date(Date.now() + expiryMinutes * 60 * 1000);
  const sas = generateBlobSASQueryParameters(
    {
      containerName: CONTAINER,
      blobName,
      permissions: BlobSASPermissions.parse('r'),
      expiresOn: expires,
    },
    creds
  ).toString();
  return `https://${accountName}.blob.core.windows.net/${CONTAINER}/${encodeBlobPath(blobName)}?${sas}`;
}

// --- Auth helpers ---

interface ClientPrincipal {
  userId: string;
  userDetails: string;
  userRoles: string[];
  identityProvider: string;
}

export function getClientPrincipal(req: HttpRequest): ClientPrincipal | null {
  const header = req.headers.get('x-ms-client-principal');
  if (!header) return null;
  try {
    const decoded = Buffer.from(header, 'base64').toString('utf-8');
    return JSON.parse(decoded);
  } catch {
    return null;
  }
}

export function isOwner(req: HttpRequest): boolean {
  if (!OWNER_USER_ID) return true; // bootstrap mode: no owner configured
  const principal = getClientPrincipal(req);
  if (!principal) return false;
  return principal.userId === OWNER_USER_ID;
}

export function requireOwner(req: HttpRequest): HttpResponseInit | null {
  if (isOwner(req)) return null;
  return { status: 403, jsonBody: { error: 'forbidden' } };
}

// --- Path helpers ---

export function isHiddenPath(name: string): boolean {
  return name.startsWith(TRASH_PREFIX) || name.startsWith(THUMB_PREFIX) || name.startsWith(SHARE_PREFIX);
}

export function trashPath(name: string): string {
  return TRASH_PREFIX + name;
}

export function thumbPath(name: string): string {
  return THUMB_PREFIX + name;
}

export function sharePath(token: string): string {
  return SHARE_PREFIX + token + '.json';
}
