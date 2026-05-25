import type { DriveFile, DriveFolder } from '../types';
import { FileIcon } from './FileIcon';
import { formatBytes, formatDate } from '../utils';

interface Props {
  folders: DriveFolder[];
  files: DriveFile[];
  onOpenFolder: (f: DriveFolder) => void;
  onPreview: (f: DriveFile) => void;
  onContext: (e: React.MouseEvent, file?: DriveFile, folder?: DriveFolder) => void;
}

export function FileList({ folders, files, onOpenFolder, onPreview, onContext }: Props) {
  return (
    <table className="list-table">
      <thead>
        <tr>
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
            <td className="list-name-cell">
              <FileIcon folder size={20} />
              <span>{f.displayName}</span>
            </td>
            <td>—</td>
            <td>—</td>
          </tr>
        ))}
        {files.map((file) => (
          <tr
            key={file.name}
            onDoubleClick={() => onPreview(file)}
            onContextMenu={(e) => onContext(e, file)}
            className="list-row"
          >
            <td className="list-name-cell">
              <FileIcon contentType={file.contentType} name={file.displayName} size={20} />
              <span>{file.displayName}</span>
            </td>
            <td>{formatDate(file.lastModified)}</td>
            <td>{formatBytes(file.size)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
