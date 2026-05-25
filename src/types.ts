export interface DriveFile {
  name: string;
  displayName: string;
  size: number;
  contentType: string;
  lastModified: string;
  metadata: Record<string, string>;
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
}

export type ViewMode = 'grid' | 'list';
