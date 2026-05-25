import { BlobServiceClient, StorageSharedKeyCredential, generateBlobSASQueryParameters, BlobSASPermissions, ContainerSASPermissions } from '@azure/storage-blob';

const CONN = process.env.AZURE_STORAGE_CONNECTION_STRING || '';
const CONTAINER = process.env.STORAGE_CONTAINER || 'drive-files';

function parseConn(): { accountName: string; accountKey: string } {
  const parts: Record<string, string> = {};
  CONN.split(';').forEach((p) => {
    const idx = p.indexOf('=');
    if (idx > 0) parts[p.substring(0, idx)] = p.substring(idx + 1);
  });
  return { accountName: parts['AccountName'], accountKey: parts['AccountKey'] };
}

export function getBlobService(): BlobServiceClient {
  return BlobServiceClient.fromConnectionString(CONN);
}

export function getContainerClient() {
  return getBlobService().getContainerClient(CONTAINER);
}

export const containerName = CONTAINER;

export function generateUploadSas(blobName: string, expiryMinutes = 30): string {
  const { accountName, accountKey } = parseConn();
  const creds = new StorageSharedKeyCredential(accountName, accountKey);
  const expires = new Date(Date.now() + expiryMinutes * 60 * 1000);
  const sas = generateBlobSASQueryParameters(
    {
      containerName: CONTAINER,
      blobName,
      permissions: BlobSASPermissions.parse('cw'),
      expiresOn: expires,
      protocol: undefined,
    },
    creds
  ).toString();
  return `https://${accountName}.blob.core.windows.net/${CONTAINER}/${encodeURIComponent(blobName)}?${sas}`;
}

export function generateReadSas(blobName: string, expiryMinutes = 60): string {
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
  return `https://${accountName}.blob.core.windows.net/${CONTAINER}/${encodeURIComponent(blobName)}?${sas}`;
}
