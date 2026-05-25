import { useState } from 'react';
import { X, Copy, Check } from 'lucide-react';
import type { DriveFile } from '../types';
import { createShare } from '../api';

interface Props {
  file: DriveFile;
  onClose: () => void;
}

export function ShareModal({ file, onClose }: Props) {
  const [token, setToken] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onCreate = async () => {
    setCreating(true);
    setError(null);
    try {
      const r = await createShare(file.name);
      setToken(r.token);
    } catch (e: any) {
      setError(e.message || 'failed');
    } finally {
      setCreating(false);
    }
  };

  const url = token ? `${window.location.origin}/share/${token}` : '';

  const copy = () => {
    navigator.clipboard.writeText(url);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal small" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title"><strong>Share "{file.displayName}"</strong></div>
          <button className="icon-btn" onClick={onClose}><X size={20} /></button>
        </div>
        <div className="modal-body share-body">
          {!token ? (
            <>
              <p>Anyone with the link can view and download this file. You can revoke the link any time.</p>
              <button className="primary-btn" onClick={onCreate} disabled={creating}>
                {creating ? 'Creating…' : 'Create share link'}
              </button>
              {error && <p className="error">{error}</p>}
            </>
          ) : (
            <>
              <p>Share link created:</p>
              <div className="share-link-row">
                <input readOnly value={url} onClick={(e) => (e.target as HTMLInputElement).select()} />
                <button className="icon-btn" onClick={copy}>
                  {copied ? <Check size={18} /> : <Copy size={18} />}
                </button>
              </div>
              <p className="hint">Tip: to revoke, right-click the file and choose "Manage shares" (coming soon) or contact admin.</p>
            </>
          )}
        </div>
      </div>
    </div>
  );
}