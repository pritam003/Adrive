import { useRef } from 'react';
import { MoreVertical, Check } from 'lucide-react';
import type { DriveFile, DriveFolder } from '../types';
import { FileIcon } from './FileIcon';
import { prefetchFile } from '../api';
import { getFileCategory } from '../utils';

interface Props {
  folders: DriveFolder[];
  files: DriveFile[];
  onOpenFolder: (f: DriveFolder) => void;
  onPreview: (f: DriveFile) => void;
  onContext: (e: React.MouseEvent, file?: DriveFile, folder?: DriveFolder) => void;
  selected: Set<string>;
  onToggleSelect: (name: string) => void;
}

export function FileGrid({ folders, files, onOpenFolder, onPreview, onContext, selected, onToggleSelect }: Props) {
  const hoverTimers = useRef<Map<string, number>>(new Map());

  const onHoverStart = (file: DriveFile) => {
    if (!file.readSasUrl) return;
    const t = window.setTimeout(() => prefetchFile(file.name, file.readSasUrl!, file.size), 150);
    hoverTimers.current.set(file.name, t);
  };
  const onHoverEnd = (file: DriveFile) => {
    const t = hoverTimers.current.get(file.name);
    if (t) clearTimeout(t);
    hoverTimers.current.delete(file.name);
  };

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
      {files.map((file) => {
        const cat = getFileCategory(file.contentType, file.displayName);
        const isSel = selected.has(file.name);
        const showThumb = (cat === 'image' || cat === 'video') && !!file.readSasUrl;
        return (
          <div
            key={file.name}
            className={'grid-item' + (isSel ? ' selected' : '')}
            onDoubleClick={() => onPreview(file)}
            onContextMenu={(e) => onContext(e, file)}
            onMouseEnter={() => onHoverStart(file)}
            onMouseLeave={() => onHoverEnd(file)}
          >
            <button
              className={'select-btn' + (isSel ? ' active' : '')}
              onClick={(e) => { e.stopPropagation(); onToggleSelect(file.name); }}
              title="Select"
            >
              <Check size={14} />
            </button>
            <button className="more-btn" onClick={(e) => { e.stopPropagation(); onContext(e, file); }}>
              <MoreVertical size={16} />
            </button>
            <div className="grid-thumb">
              {showThumb && cat === 'image' ? (
                <img
                  src={file.readSasUrl}
                  alt=""
                  className="grid-thumb-img"
                  loading="lazy"
                  onError={(e) => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
                />
              ) : (
                <FileIcon contentType={file.contentType} name={file.displayName} size={56} />
              )}
            </div>
            <div className="grid-name" title={file.displayName}>{file.displayName}</div>
          </div>
        );
      })}
    </div>
  );
}
