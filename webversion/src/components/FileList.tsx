import type { DriveFile, DriveFolder } from '../types';
import { FileIcon } from './FileIcon';
import { formatBytes, formatDate } from '../utils';
import { prefetchFile } from '../api';
import { useRef } from 'react';

interface Props {
  folders: DriveFolder[];
  files: DriveFile[];
  onOpenFolder: (f: DriveFolder) => void;
  onPreview: (f: DriveFile) => void;
  onContext: (e: React.MouseEvent, file?: DriveFile, folder?: DriveFolder) => void;
  selected: Set<string>;
  onToggleSelect: (name: string) => void;
}

export function FileList({ folders, files, onOpenFolder, onPreview, onContext, selected, onToggleSelect }: Props) {
  const hoverTimers = useRef<Map<string, number>>(new Map());

  return (
    <table className="list-table">
      <thead>
        <tr>
          <th style={{ width: 32 }}></th>
          <th>Name</th>
          <th>Modified</th>
          <th>Size</th>
        </tr>
      </thead>
      <tbody>
        {folders.map((f) => (
          <tr
            key={f.name}
            onDoubleClick={() => onOpenFolder(f)}
            onContextMenu={(e) => onContext(e, undefined, f)}
            className="list-row"
          >
            <td></td>
            <td className="list-name-cell">
              <FileIcon folder size={20} />
              <span>{f.displayName}</span>
            </td>
            <td>—</td>
            <td>—</td>
          </tr>
        ))}
        {files.map((file) => {
          const isSel = selected.has(file.name);
          return (
            <tr
              key={file.name}
              onDoubleClick={() => onPreview(file)}
              onContextMenu={(e) => onContext(e, file)}
              className={'list-row' + (isSel ? ' selected' : '')}
              onMouseEnter={() => {
                if (!file.readSasUrl) return;
                const t = window.setTimeout(() => prefetchFile(file.name, file.readSasUrl!, file.size), 150);
                hoverTimers.current.set(file.name, t);
              }}
              onMouseLeave={() => {
                const t = hoverTimers.current.get(file.name);
                if (t) clearTimeout(t);
                hoverTimers.current.delete(file.name);
              }}
            >
              <td>
                <input
                  type="checkbox"
                  checked={isSel}
                  onChange={() => onToggleSelect(file.name)}
                  onClick={(e) => e.stopPropagation()}
                />
              </td>
              <td className="list-name-cell">
                <FileIcon contentType={file.contentType} name={file.displayName} size={20} />
                <span>{file.displayName}</span>
              </td>
              <td>{formatDate(file.lastModified)}</td>
              <td>{formatBytes(file.size)}</td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
