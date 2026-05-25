import { useEffect, useRef, useState } from 'react';
import { Plus, FolderPlus, FolderUp, FileUp, HardDrive, Trash, LogOut, Infinity as InfinityIcon, ChevronDown } from 'lucide-react';
import type { QuotaInfo, MeResponse } from '../types';
import { formatBytes } from '../utils';
import { logout } from '../api';

interface Props {
  onUploadFiles: () => void;
  onUploadFolder: () => void;
  onNewFolder: () => void;
  quota: QuotaInfo;
  view: 'drive' | 'trash';
  onChangeView: (v: 'drive' | 'trash') => void;
  me: MeResponse | null;
}

export function Sidebar({ onUploadFiles, onUploadFolder, onNewFolder, quota, view, onChangeView, me }: Props) {
  const [open, setOpen] = useState(false);
  const wrapRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onDoc = (e: MouseEvent) => {
      if (!wrapRef.current?.contains(e.target as Node)) setOpen(false);
    };
    window.addEventListener('click', onDoc);
    return () => window.removeEventListener('click', onDoc);
  }, [open]);

  const pick = (fn: () => void) => () => { setOpen(false); fn(); };

  return (
    <aside className="sidebar">
      <div className="logo">
        <HardDrive size={28} className="logo-icon" />
        <span>Adrive</span>
      </div>

      <div className="new-menu" ref={wrapRef}>
        <button
          className="new-btn"
          onClick={(e) => { e.stopPropagation(); setOpen((v) => !v); }}
          aria-haspopup="menu"
          aria-expanded={open}
        >
          <Plus size={20} />
          <span>New upload</span>
          <ChevronDown size={16} className={'new-btn-caret' + (open ? ' open' : '')} />
        </button>
        {open && (
          <div className="new-popover" role="menu">
            <button className="new-popover-item" onClick={pick(onUploadFiles)} role="menuitem">
              <FileUp size={18} />
              <span>Upload files</span>
            </button>
            <button className="new-popover-item" onClick={pick(onUploadFolder)} role="menuitem">
              <FolderUp size={18} />
              <span>Upload folder</span>
            </button>
            <div className="new-popover-sep" />
            <button className="new-popover-item" onClick={pick(onNewFolder)} role="menuitem">
              <FolderPlus size={18} />
              <span>New folder</span>
            </button>
          </div>
        )}
      </div>

      <nav className="sidebar-nav">
        <a
          className={'nav-item' + (view === 'drive' ? ' active' : '')}
          onClick={() => onChangeView('drive')}
          style={{ cursor: 'pointer' }}
        >
          <HardDrive size={18} />
          <span>My Drive</span>
        </a>
        <a
          className={'nav-item' + (view === 'trash' ? ' active' : '')}
          onClick={() => onChangeView('trash')}
          style={{ cursor: 'pointer' }}
        >
          <Trash size={18} />
          <span>Trash{quota.trashCount ? ` (${quota.trashCount})` : ''}</span>
        </a>
      </nav>

      <div className="storage">
        <div className="storage-label">
          <span className="storage-used">{formatBytes(quota.totalBytes)}</span>
          <span className="storage-of">of</span>
          <span className="storage-infinity" title="Unlimited storage">
            <InfinityIcon size={20} strokeWidth={2.5} />
          </span>
        </div>
        <div className="storage-bar">
          <div className="storage-bar-rainbow" />
        </div>
        <div className="storage-info">
          {quota.fileCount} {quota.fileCount === 1 ? 'file' : 'files'} · unlimited ✨
        </div>
      </div>

      {me && me.authenticated && (
        <div className="user-info">
          <div className="user-name" title={me.userDetails}>{me.userDetails}</div>
          <button
            type="button"
            onClick={async () => {
              await logout();
              window.location.reload();
            }}
            className="logout-link"
            title="Sign out"
          >
            <LogOut size={14} /> Sign out
          </button>
        </div>
      )}
    </aside>
  );
}
