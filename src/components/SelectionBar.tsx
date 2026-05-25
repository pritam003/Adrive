import { Download, Trash2, X, Archive } from 'lucide-react';
import type { DriveFile } from '../types';
import { formatBytes } from '../utils';

interface Props {
  files: DriveFile[];
  onClear: () => void;
  onDownload: () => void;
  onDownloadZip: () => void;
  onDelete: () => void;
}

export function SelectionBar({ files, onClear, onDownload, onDownloadZip, onDelete }: Props) {
  const total = files.reduce((s, f) => s + f.size, 0);
  return (
    <div className="selection-bar">
      <div className="selection-info">
        <button className="icon-btn" onClick={onClear} title="Clear selection"><X size={18} /></button>
        <span>{files.length} selected · {formatBytes(total)}</span>
      </div>
      <div className="selection-actions">
        {files.length === 1 && (
          <button onClick={onDownload}><Download size={16} /> Download</button>
        )}
        {files.length > 1 && (
          <button onClick={onDownloadZip}><Archive size={16} /> Download as ZIP</button>
        )}
        <button className="danger" onClick={onDelete}><Trash2 size={16} /> Move to trash</button>
      </div>
    </div>
  );
}