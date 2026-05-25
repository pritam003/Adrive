export interface DriveFile {
  name: string;
  displayName: string;
  size: number;
  contentType: string;
  lastModified: string;
  metadata: Record<string, string>;
  readSasUrl?: string;
  thumbnailUrl?: string;
}

export interface DriveFolder {
  name: string;
  displayName: string;
}

export interface ListResponse {
  folders: DriveFolder[];
  files: DriveFile[];
}

export interface QuotaInfo {
  totalBytes: number;
  fileCount: number;
  trashBytes?: number;
  trashCount?: number;
}

export interface TrashItem {
  trashKey: string;
  originalPath: string;
  size: number;
  contentType: string;
  deletedAt: number | null;
  readSasUrl: string;
}

export interface MeResponse {
  authenticated: boolean;
  userId?: string;
  userDetails?: string;
  identityProvider?: string;
  isOwner?: boolean;
  ownerConfigured: boolean;
}

export interface ShareCreateResponse {
  token: string;
  name: string;
}

export interface ShareInfo {
  token: string;
  displayName: string;
  size: number;
  contentType: string;
  sasUrl: string;
  thumbnailUrl?: string;
}

export type ViewMode = 'grid' | 'list';
