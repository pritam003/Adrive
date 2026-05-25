import { useState, useEffect, useCallback, useRef } from 'react';
import { Sidebar } from './components/Sidebar';
import { Topbar } from './components/Topbar';
import { Breadcrumbs } from './components/Breadcrumbs';
import { FileGrid } from './components/FileGrid';
import { FileList } from './components/FileList';
import { UploadProgress } from './components/UploadProgress';
import { PreviewModal } from './components/PreviewModal';
import { ContextMenu } from './components/ContextMenu';
import { NewFolderDialog } from './components/NewFolderDialog';
import { listItems, uploadFile, deleteFile as apiDelete, getReadSas, createFolder as apiCreateFolder, renameFile, getQuota } from './api';
import type { DriveFile, DriveFolder, ViewMode, QuotaInfo } from './types';
import './App.css';

interface UploadItem {
  id: string;
  name: string;
  progress: number;
  status: 'uploading' | 'done' | 'error';
}

interface PreviewState {
  file: DriveFile;
  url: string;
}

interface MenuState {
  x: number;
  y: number;
  file?: DriveFile;
}

export default function App() {
  const [prefix, setPrefix] = useState('');
  const [folders, setFolders] = useState<DriveFolder[]>([]);
  const [files, setFiles] = useState<DriveFile[]>([]);
  const [view, setView] = useState<ViewMode>('grid');
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [uploads, setUploads] = useState<UploadItem[]>([]);
  const [preview, setPreview] = useState<PreviewState | null>(null);
  const [menu, setMenu] = useState<MenuState | null>(null);
  const [newFolderOpen, setNewFolderOpen] = useState(false);
  const [quota, setQuota] = useState<QuotaInfo>({ totalBytes: 0, fileCount: 0 });
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const data = await listItems(prefix);
      setFolders(data.folders);
      setFiles(data.files.filter((f) => !f.displayName.endsWith('.keep')));
      const q = await getQuota();
      setQuota(q);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, [prefix]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  useEffect(() => {
    const close = () => setMenu(null);
    window.addEventListener('click', close);
    return () => window.removeEventListener('click', close);
  }, []);

  const handleUpload = async (fileList: FileList | File[]) => {
    const filesArr = Array.from(fileList);
    for (const file of filesArr) {
      const id = `${Date.now()}-${Math.random()}`;
      const blobName = `${prefix}${file.name}`;
      setUploads((u) => [...u, { id, name: file.name, progress: 0, status: 'uploading' }]);
      try {
        await uploadFile(file, blobName, (loaded, total) => {
          setUploads((u) => u.map((it) => (it.id === id ? { ...it, progress: (loaded / total) * 100 } : it)));
        });
        setUploads((u) => u.map((it) => (it.id === id ? { ...it, progress: 100, status: 'done' } : it)));
      } catch (e) {
        console.error(e);
        setUploads((u) => u.map((it) => (it.id === id ? { ...it, status: 'error' } : it)));
      }
    }
    setTimeout(() => setUploads((u) => u.filter((it) => it.status === 'uploading')), 3000);
    refresh();
  };

  const handleDelete = async (file: DriveFile) => {
    if (!confirm(`Delete "${file.displayName}"?`)) return;
    await apiDelete(file.name);
    refresh();
  };

  const handleRename = async (file: DriveFile) => {
    const newName = prompt('New name:', file.displayName);
    if (!newName || newName === file.displayName) return;
    const newPath = prefix + newName;
    await renameFile(file.name, newPath);
    refresh();
  };

  const handleDownload = async (file: DriveFile) => {
    const url = await getReadSas(file.name);
    const a = document.createElement('a');
    a.href = url;
    a.download = file.displayName;
    document.body.appendChild(a);
    a.click();
    a.remove();
  };

  const handlePreview = async (file: DriveFile) => {
    const url = await getReadSas(file.name);
    setPreview({ file, url });
  };

  const handleOpenFolder = (folder: DriveFolder) => {
    setPrefix(folder.name);
  };

  const handleNewFolder = async (name: string) => {
    if (!name.trim()) return;
    await apiCreateFolder(prefix + name.trim());
    setNewFolderOpen(false);
    refresh();
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    if (e.dataTransfer.files.length) handleUpload(e.dataTransfer.files);
  };

  const filteredFolders = folders.filter((f) => f.displayName.toLowerCase().includes(search.toLowerCase()));
  const filteredFiles = files.filter((f) => f.displayName.toLowerCase().includes(search.toLowerCase()));

  return (
    <div
      className="app"
      onDragOver={(e) => {
        e.preventDefault();
        setDragOver(true);
      }}
      onDragLeave={() => setDragOver(false)}
      onDrop={handleDrop}
    >
      <Sidebar
        onNew={() => fileInputRef.current?.click()}
        onNewFolder={() => setNewFolderOpen(true)}
        quota={quota}
      />
      <div className="main">
        <Topbar search={search} setSearch={setSearch} view={view} setView={setView} />
        <Breadcrumbs prefix={prefix} onNavigate={setPrefix} />
        <div className="content">
          {loading ? (
            <div className="empty">Loading…</div>
          ) : filteredFolders.length === 0 && filteredFiles.length === 0 ? (
            <div className="empty">
              <p>This folder is empty</p>
              <p className="hint">Drag files here or click + New to upload</p>
            </div>
          ) : view === 'grid' ? (
            <FileGrid
              folders={filteredFolders}
              files={filteredFiles}
              onOpenFolder={handleOpenFolder}
              onPreview={handlePreview}
              onContext={(e, file) => {
                e.preventDefault();
                setMenu({ x: e.clientX, y: e.clientY, file });
              }}
            />
          ) : (
            <FileList
              folders={filteredFolders}
              files={filteredFiles}
              onOpenFolder={handleOpenFolder}
              onPreview={handlePreview}
              onContext={(e, file) => {
                e.preventDefault();
                setMenu({ x: e.clientX, y: e.clientY, file });
              }}
            />
          )}
        </div>
      </div>

      <input
        ref={fileInputRef}
        type="file"
        multiple
        style={{ display: 'none' }}
        onChange={(e) => e.target.files && handleUpload(e.target.files)}
      />

      {uploads.length > 0 && <UploadProgress items={uploads} />}
      {preview && <PreviewModal file={preview.file} url={preview.url} onClose={() => setPreview(null)} />}
      {menu && menu.file && (
        <ContextMenu
          x={menu.x}
          y={menu.y}
          file={menu.file}
          onDownload={() => menu.file && handleDownload(menu.file)}
          onPreview={() => menu.file && handlePreview(menu.file)}
          onRename={() => menu.file && handleRename(menu.file)}
          onDelete={() => menu.file && handleDelete(menu.file)}
          onClose={() => setMenu(null)}
        />
      )}
      {newFolderOpen && <NewFolderDialog onSubmit={handleNewFolder} onClose={() => setNewFolderOpen(false)} />}
      {dragOver && (
        <div className="drop-overlay">
          <div className="drop-message">Drop files to upload</div>
        </div>
      )}
    </div>
  );
}
