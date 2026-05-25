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
import { ShareModal } from './components/ShareModal';
import { TrashView } from './components/TrashView';
import { SelectionBar } from './components/SelectionBar';
import { AuthGate } from './AuthGate';
import {
  listItems, uploadFile, deleteFile as apiDelete, getReadSasCached,
  createFolder as apiCreateFolder, renameFile, getQuota,
} from './api';
import { generateThumbnail, uploadThumbnail } from './thumbnail';
import { downloadAsZip } from './zipDownload';
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
  thumbnailUrl?: string;
}

interface MenuState {
  x: number;
  y: number;
  file?: DriveFile;
}

function AppInner() {
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
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [shareFor, setShareFor] = useState<DriveFile | null>(null);
  const [navView, setNavView] = useState<'drive' | 'trash'>('drive');
  const [me, setMe] = useState<any>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const folderInputRef = useRef<HTMLInputElement>(null);

  const refresh = useCallback(async () => {
    if (navView !== 'drive') return;
    setLoading(true);
    try {
      const data = await listItems(prefix);
      setFolders(data.folders);
      setFiles(data.files);
      const q = await getQuota();
      setQuota(q);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, [prefix, navView]);

  useEffect(() => { refresh(); }, [refresh]);

  // refresh quota when on trash view too
  useEffect(() => {
    getQuota().then(setQuota).catch(() => {});
  }, [navView]);

  useEffect(() => {
    const close = () => setMenu(null);
    window.addEventListener('click', close);
    return () => window.removeEventListener('click', close);
  }, []);

  // Fetch /api/me once to show user info
  useEffect(() => {
    fetch('/api/me').then((r) => r.json()).then(setMe).catch(() => {});
  }, []);

  const handleUpload = async (fileList: FileList | File[], relativePaths?: string[]) => {
    const filesArr = Array.from(fileList);
    // Preserve nested folder structure if available (folder picker or drag-drop dir)
    const pathFor = (f: File, i: number): string => {
      const explicit = relativePaths?.[i];
      if (explicit) return explicit;
      // From <input webkitdirectory>
      const w = (f as File & { webkitRelativePath?: string }).webkitRelativePath;
      return w && w.length > 0 ? w : f.name;
    };
    // Collect unique parent folders we need to create first so they show up in /api/list
    const folderSet = new Set<string>();
    for (let i = 0; i < filesArr.length; i++) {
      const rel = pathFor(filesArr[i], i);
      const parts = rel.split('/');
      // every prefix except the file itself
      for (let k = 1; k < parts.length; k++) {
        folderSet.add(parts.slice(0, k).join('/'));
      }
    }
    // Create folder placeholders (best-effort, parallel)
    await Promise.all(
      Array.from(folderSet).map((dir) => apiCreateFolder(`${prefix}${dir}`).catch(() => {}))
    );
    for (let i = 0; i < filesArr.length; i++) {
      const file = filesArr[i];
      const rel = pathFor(file, i);
      const id = `${Date.now()}-${Math.random()}`;
      const blobName = `${prefix}${rel}`;
      setUploads((u) => [...u, { id, name: rel, progress: 0, status: 'uploading' }]);
      try {
        await uploadFile(file, blobName, (loaded, total) => {
          setUploads((u) => u.map((it) => (it.id === id ? { ...it, progress: (loaded / total) * 100 } : it)));
        });
        setUploads((u) => u.map((it) => (it.id === id ? { ...it, progress: 100, status: 'done' } : it)));
        // Best-effort thumbnail generation in background
        generateThumbnail(file)
          .then((blob) => (blob ? uploadThumbnail(blob, blobName) : null))
          .catch(() => {});
      } catch (e) {
        console.error(e);
        setUploads((u) => u.map((it) => (it.id === id ? { ...it, status: 'error' } : it)));
      }
    }
    setTimeout(() => setUploads((u) => u.filter((it) => it.status === 'uploading')), 3000);
    refresh();
  };

  const handleDelete = async (file: DriveFile) => {
    if (!confirm(`Move "${file.displayName}" to trash?`)) return;
    await apiDelete(file.name);
    setSelected((s) => { const n = new Set(s); n.delete(file.name); return n; });
    refresh();
  };

  const handleRename = async (file: DriveFile) => {
    const newName = prompt('New name:', file.displayName);
    if (!newName || newName === file.displayName) return;
    const newPath = prefix + newName;
    await renameFile(file.name, newPath);
    refresh();
  };

  const handleDownload = (file: DriveFile) => {
    // Use cached/embedded SAS — instant; no API call
    const url = file.readSasUrl;
    if (!url) return;
    const a = document.createElement('a');
    a.href = url;
    a.download = file.displayName;
    document.body.appendChild(a);
    a.click();
    a.remove();
  };

  const handlePreview = (file: DriveFile) => {
    // Open INSTANTLY with embedded SAS (skeleton inside modal handles load state)
    let url = file.readSasUrl;
    if (url) {
      setPreview({ file, url, thumbnailUrl: file.thumbnailUrl });
      return;
    }
    // Fallback if list response somehow lacked SAS
    getReadSasCached(file.name).then((u) => setPreview({ file, url: u, thumbnailUrl: file.thumbnailUrl }));
  };

  const handleOpenFolder = (folder: DriveFolder) => {
    setPrefix(folder.name);
    setSelected(new Set());
  };

  const handleNewFolder = async (name: string) => {
    if (!name.trim()) return;
    await apiCreateFolder(prefix + name.trim());
    setNewFolderOpen(false);
    refresh();
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const items = e.dataTransfer.items;
    // If the OS gave us directory entries, walk them so folders upload too.
    if (items && items.length && typeof (items[0] as DataTransferItem).webkitGetAsEntry === 'function') {
      const collected: { file: File; path: string }[] = [];
      const walkEntry = async (entry: FileSystemEntry, parentPath: string): Promise<void> => {
        if (entry.isFile) {
          const f: File = await new Promise((resolve, reject) =>
            (entry as FileSystemFileEntry).file(resolve, reject)
          );
          collected.push({ file: f, path: `${parentPath}${f.name}` });
          return;
        }
        if (entry.isDirectory) {
          const reader = (entry as FileSystemDirectoryEntry).createReader();
          // readEntries returns batches; loop until empty
          const readAll = (): Promise<FileSystemEntry[]> =>
            new Promise((resolve, reject) => {
              const all: FileSystemEntry[] = [];
              const next = () =>
                reader.readEntries((batch) => {
                  if (!batch.length) return resolve(all);
                  all.push(...batch);
                  next();
                }, reject);
              next();
            });
          const children = await readAll();
          await Promise.all(children.map((c) => walkEntry(c, `${parentPath}${entry.name}/`)));
        }
      };
      const entries: FileSystemEntry[] = [];
      for (let i = 0; i < items.length; i++) {
        const ent = items[i].webkitGetAsEntry();
        if (ent) entries.push(ent);
      }
      if (entries.length) {
        await Promise.all(entries.map((ent) => walkEntry(ent, '')));
        if (collected.length) {
          handleUpload(
            collected.map((c) => c.file),
            collected.map((c) => c.path)
          );
          return;
        }
      }
    }
    if (e.dataTransfer.files.length) handleUpload(e.dataTransfer.files);
  };

  const toggleSelect = (name: string) => {
    setSelected((s) => {
      const n = new Set(s);
      if (n.has(name)) n.delete(name); else n.add(name);
      return n;
    });
  };

  const selectedFiles = files.filter((f) => selected.has(f.name));

  const handleSelectedDownloadZip = async () => {
    if (selectedFiles.length === 0) return;
    const id = `zip-${Date.now()}`;
    setUploads((u) => [...u, { id, name: `${selectedFiles.length} files.zip`, progress: 0, status: 'uploading' }]);
    try {
      await downloadAsZip(selectedFiles, `adrive-${Date.now()}.zip`, (loaded, total) => {
        setUploads((u) => u.map((it) => (it.id === id ? { ...it, progress: (loaded / total) * 100 } : it)));
      });
      setUploads((u) => u.map((it) => (it.id === id ? { ...it, progress: 100, status: 'done' } : it)));
    } catch (e) {
      console.error(e);
      setUploads((u) => u.map((it) => (it.id === id ? { ...it, status: 'error' } : it)));
    }
    setTimeout(() => setUploads((u) => u.filter((it) => it.id !== id)), 3000);
  };

  const handleSelectedDelete = async () => {
    if (selectedFiles.length === 0) return;
    if (!confirm(`Move ${selectedFiles.length} items to trash?`)) return;
    for (const f of selectedFiles) {
      try { await apiDelete(f.name); } catch (e) { console.error(e); }
    }
    setSelected(new Set());
    refresh();
  };

  const filteredFolders = folders.filter((f) => f.displayName.toLowerCase().includes(search.toLowerCase()));
  const filteredFiles = files.filter((f) => f.displayName.toLowerCase().includes(search.toLowerCase()));

  return (
    <div
      className="app"
      onDragOver={(e) => {
        if (navView !== 'drive') return;
        e.preventDefault();
        setDragOver(true);
      }}
      onDragLeave={() => setDragOver(false)}
      onDrop={handleDrop}
    >
      <Sidebar
        onNew={() => fileInputRef.current?.click()}
        onNewFolder={() => setNewFolderOpen(true)}
        onUploadFolder={() => folderInputRef.current?.click()}
        quota={quota}
        view={navView}
        onChangeView={(v) => { setNavView(v); setSelected(new Set()); }}
        me={me}
      />
      <div className="main">
        {navView === 'drive' ? (
          <>
            <Topbar search={search} setSearch={setSearch} view={view} setView={setView} />
            <Breadcrumbs prefix={prefix} onNavigate={(p) => { setPrefix(p); setSelected(new Set()); }} />
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
                  selected={selected}
                  onToggleSelect={toggleSelect}
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
                  selected={selected}
                  onToggleSelect={toggleSelect}
                />
              )}
            </div>
          </>
        ) : (
          <>
            <div className="topbar"><h2 style={{ margin: '0 24px' }}>Trash</h2></div>
            <div className="content">
              <TrashView onChange={() => getQuota().then(setQuota).catch(() => {})} />
            </div>
          </>
        )}
      </div>

      <input
        ref={fileInputRef}
        type="file"
        multiple
        style={{ display: 'none' }}
        onChange={(e) => {
          if (e.target.files) handleUpload(e.target.files);
          e.target.value = '';
        }}
      />
      <input
        ref={folderInputRef}
        type="file"
        // @ts-expect-error – non-standard but widely supported
        webkitdirectory=""
        directory=""
        multiple
        style={{ display: 'none' }}
        onChange={(e) => {
          if (e.target.files) handleUpload(e.target.files);
          e.target.value = '';
        }}
      />

      {selectedFiles.length > 0 && navView === 'drive' && (
        <SelectionBar
          files={selectedFiles}
          onClear={() => setSelected(new Set())}
          onDownload={() => handleDownload(selectedFiles[0])}
          onDownloadZip={handleSelectedDownloadZip}
          onDelete={handleSelectedDelete}
        />
      )}

      {uploads.length > 0 && <UploadProgress items={uploads} />}
      {preview && (
        <PreviewModal
          file={preview.file}
          url={preview.url}
          thumbnailUrl={preview.thumbnailUrl}
          onClose={() => setPreview(null)}
        />
      )}
      {menu && menu.file && (
        <ContextMenu
          x={menu.x}
          y={menu.y}
          file={menu.file}
          onDownload={() => menu.file && handleDownload(menu.file)}
          onPreview={() => menu.file && handlePreview(menu.file)}
          onRename={() => menu.file && handleRename(menu.file)}
          onDelete={() => menu.file && handleDelete(menu.file)}
          onShare={() => { if (menu.file) setShareFor(menu.file); setMenu(null); }}
          onClose={() => setMenu(null)}
        />
      )}
      {shareFor && <ShareModal file={shareFor} onClose={() => setShareFor(null)} />}
      {newFolderOpen && <NewFolderDialog onSubmit={handleNewFolder} onClose={() => setNewFolderOpen(false)} />}
      {dragOver && (
        <div className="drop-overlay">
          <div className="drop-message">Drop files to upload</div>
        </div>
      )}
    </div>
  );
}

export default function App() {
  return (
    <AuthGate>
      {() => <AppInner />}
    </AuthGate>
  );
}
