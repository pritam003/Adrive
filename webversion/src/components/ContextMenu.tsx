import { Eye, Download, Edit2, Trash2, Share2 } from 'lucide-react';
import type { DriveFile } from '../types';

interface Props {
  x: number;
  y: number;
  file: DriveFile;
  onPreview: () => void;
  onDownload: () => void;
  onRename: () => void;
  onDelete: () => void;
  onShare: () => void;
  onClose: () => void;
}

export function ContextMenu({ x, y, onPreview, onDownload, onRename, onDelete, onShare }: Props) {
  const style = { top: y, left: x };
  return (
    <div className="context-menu" style={style} onClick={(e) => e.stopPropagation()}>
      <button onClick={onPreview}><Eye size={16} /> Preview</button>
      <button onClick={onDownload}><Download size={16} /> Download</button>
      <button onClick={onShare}><Share2 size={16} /> Share link…</button>
      <button onClick={onRename}><Edit2 size={16} /> Rename</button>
      <div className="menu-sep" />
      <button onClick={onDelete} className="danger"><Trash2 size={16} /> Move to trash</button>
    </div>
  );
}
