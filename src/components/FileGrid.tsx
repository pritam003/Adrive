import { MoreVertical } from 'lucide-react';
import type { DriveFile, DriveFolder } from '../types';
import { FileIcon } from './FileIcon';

interface Props {
  folders: DriveFolder[];
  files: DriveFile[];
  onOpenFolder: (f: DriveFolder) => void;
  onPreview: (f: DriveFile) => void;
  onContext: (e: React.MouseEvent, file?: DriveFile, folder?: DriveFolder) => void;
}

export function FileGrid({ folders, files, onOpenFolder, onPreview, onContext }: Props) {
  return (
    <div className="grid">
      {folders.map((f) => (
        <div
          key={f.name}
          className="grid-item folder"
          onDoubleClick={() => onOpenFolder(f)}
          onContextMenu={(e) => onContext(e, undefined, f)}
        >
          <div className="grid-thumb">
            <FileIcon folder size={56} />
          </div>
          <div className="grid-name" title={f.displayName}>{f.displayName}</div>
        </div>
      ))}
      {files.map((file) => (
        <div
          key={file.name}
          className="grid-item"
          onDoubleClick={() => onPreview(file)}
          onContextMenu={(e) => onContext(e, file)}
        >
          <button className="more-btn" onClick={(e) => { e.stopPropagation(); onContext(e, file); }}>
            <MoreVertical size={16} />
          </button>
          <div className="grid-thumb">
            <FileIcon contentType={file.contentType} name={file.displayName} size={56} />
          </div>
          <div className="grid-name" title={file.displayName}>{file.displayName}</div>
        </div>
      ))}
    </div>
  );
}
