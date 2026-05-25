import { useEffect, useState } from 'react';
import { RotateCcw, Trash2, AlertTriangle } from 'lucide-react';
import type { TrashItem } from '../types';
import { listTrash, restoreFromTrash, purgeTrashItem, purgeAllTrash } from '../api';
import { formatBytes, formatDate } from '../utils';
import { FileIcon } from './FileIcon';

interface Props {
  onChange: () => void;
}

export function TrashView({ onChange }: Props) {
  const [items, setItems] = useState<TrashItem[] | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  const refresh = async () => {
    setItems(null);
    const data = await listTrash();
    data.sort((a, b) => (b.deletedAt || 0) - (a.deletedAt || 0));
    setItems(data);
  };

  useEffect(() => { refresh(); }, []);

  const onRestore = async (it: TrashItem) => {
    setBusy(it.trashKey);
    await restoreFromTrash(it.trashKey);
    setBusy(null);
    refresh();
    onChange();
  };

  const onPurge = async (it: TrashItem) => {
    if (!confirm(`Permanently delete "${it.originalPath}"? This cannot be undone.`)) return;
    setBusy(it.trashKey);
    await purgeTrashItem(it.trashKey);
    setBusy(null);
    refresh();
    onChange();
  };

  const onPurgeAll = async () => {
    if (!items || items.length === 0) return;
    if (!confirm(`Permanently delete ALL ${items.length} items in trash? This cannot be undone.`)) return;
    await purgeAllTrash();
    refresh();
    onChange();
  };

  if (items === null) return <div className="empty">Loading…</div>;
  if (items.length === 0) return <div className="empty"><p>Trash is empty</p></div>;

  return (
    <div className="trash-view">
      <div className="trash-toolbar">
        <div className="trash-note"><AlertTriangle size={16} /> Items in trash stay here until you purge them.</div>
        <button className="danger-btn" onClick={onPurgeAll}>Empty trash</button>
      </div>
      <table className="list-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Deleted</th>
            <th>Size</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {items.map((it) => (
            <tr key={it.trashKey} className="list-row">
              <td className="list-name-cell">
                <FileIcon contentType={it.contentType} name={it.originalPath} size={20} />
                <span>{it.originalPath}</span>
              </td>
              <td>{it.deletedAt ? formatDate(new Date(it.deletedAt).toISOString()) : '—'}</td>
              <td>{formatBytes(it.size)}</td>
              <td className="trash-actions">
                <button title="Restore" onClick={() => onRestore(it)} disabled={busy === it.trashKey}>
                  <RotateCcw size={16} />
                </button>
                <button title="Purge" className="danger" onClick={() => onPurge(it)} disabled={busy === it.trashKey}>
                  <Trash2 size={16} />
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}