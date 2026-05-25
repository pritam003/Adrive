import { BlobServiceClient, StorageSharedKeyCredential, generateBlobSASQueryParameters, BlobSASPermissions } from '@azure/storage-blob';

const CONN = process.env.AZURE_STORAGE_CONNECTION_STRING || '';
const CONTAINER = process.env.STORAGE_CONTAINER || 'drive-files';

export const TRASH_PREFIX = '.trash/';
export const THUMB_PREFIX = '.thumbnails/';
export const SHARE_PREFIX = '.shares/';

// Re-export auth helpers so existing callers keep working.
export { OWNER_USER_ID, isOwner, requireOwner, getUserFromRequest } from './auth';

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
