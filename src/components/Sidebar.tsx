import { Plus, FolderPlus, HardDrive } from 'lucide-react';
import type { QuotaInfo } from '../types';
import { formatBytes } from '../utils';

interface Props {
  onNew: () => void;
  onNewFolder: () => void;
  quota: QuotaInfo;
}

const QUOTA_LIMIT = 5 * 1024 * 1024 * 1024; // 5GB soft cap

export function Sidebar({ onNew, onNewFolder, quota }: Props) {
  const pct = Math.min(100, (quota.totalBytes / QUOTA_LIMIT) * 100);

  return (
    <aside className="sidebar">
      <div className="logo">
        <HardDrive size={28} className="logo-icon" />
        <span>Adrive</span>
      </div>

      <div className="new-menu">
        <button className="new-btn" onClick={onNew}>
          <Plus size={20} />
          <span>New upload</span>
        </button>
        <button className="new-folder-btn" onClick={onNewFolder}>
          <FolderPlus size={18} />
          <span>New folder</span>
        </button>
      </div>

      <nav className="sidebar-nav">
        <a className="nav-item active">
          <HardDrive size={18} />
          <span>My Drive</span>
        </a>
      </nav>

      <div className="storage">
        <div className="storage-label">
          {formatBytes(quota.totalBytes)} of {formatBytes(QUOTA_LIMIT)} used
        </div>
        <div className="storage-bar">
          <div className="storage-bar-fill" style={{ width: `${pct}%` }} />
        </div>
        <div className="storage-info">{quota.fileCount} files</div>
      </div>
    </aside>
  );
}
