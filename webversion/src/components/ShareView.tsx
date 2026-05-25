import { useEffect, useState } from 'react';
import { HardDrive, Download } from 'lucide-react';
import { getShareInfo } from '../api';
import type { ShareInfo } from '../types';
import { formatBytes, getFileCategory } from '../utils';

export function ShareView() {
  const [info, setInfo] = useState<ShareInfo | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // path is /share/<token>
    const m = window.location.pathname.match(/^\/share\/([a-f0-9]{32})$/i);
    if (!m) {
      setError('Invalid share link');
      return;
    }
    getShareInfo(m[1])
      .then(setInfo)
      .catch((e) => setError(e.message || 'Share unavailable'));
  }, []);

  if (error) {
    return (
      <div className="share-page">
        <div className="share-card">
          <h1>Share unavailable</h1>
          <p>{error}</p>
        </div>
      </div>
    );
  }
  if (!info) return <div className="share-page"><div className="share-card"><p>Loading…</p></div></div>;

  const cat = getFileCategory(info.contentType, info.displayName);
  return (
    <div className="share-page">
      <header className="share-header">
        <div className="logo"><HardDrive size={22} className="logo-icon" /><span>Adrive</span></div>
      </header>
      <div className="share-content">
        <div className="share-meta">
          <h1>{info.displayName}</h1>
          <p>{formatBytes(info.size)} • {info.contentType}</p>
          <a href={info.sasUrl} download={info.displayName} className="primary-btn">
            <Download size={18} /> Download
          </a>
        </div>
        <div className="share-preview">
          {cat === 'image' && <img src={info.sasUrl} alt={info.displayName} />}
          {cat === 'video' && <video src={info.sasUrl} controls preload="metadata" poster={info.thumbnailUrl} />}
          {cat === 'audio' && <audio src={info.sasUrl} controls />}
          {cat === 'pdf' && <iframe src={info.sasUrl} title={info.displayName} />}
          {(cat === 'text' || cat === 'code') && <iframe src={info.sasUrl} title={info.displayName} />}
          {cat === 'other' && (
            <div className="preview-fallback">
              <p>Preview not available</p>
              <p className="hint">Click Download to view this file</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}