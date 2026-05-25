import { X, Download } from 'lucide-react';
import type { DriveFile } from '../types';
import { getFileCategory, formatBytes } from '../utils';

interface Props {
  file: DriveFile;
  url: string;
  onClose: () => void;
}

export function PreviewModal({ file, url, onClose }: Props) {
  const cat = getFileCategory(file.contentType, file.displayName);

  const renderBody = () => {
    if (cat === 'image') return <img src={url} alt={file.displayName} />;
    if (cat === 'video') return <video src={url} controls autoPlay />;
    if (cat === 'audio') return <audio src={url} controls autoPlay />;
    if (cat === 'pdf') return <iframe src={url} title={file.displayName} />;
    if (cat === 'text' || cat === 'code') return <iframe src={url} title={file.displayName} />;
    return (
      <div className="preview-fallback">
        <p>Preview not available</p>
        <p className="hint">Click download to view this file</p>
      </div>
    );
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title">
            <strong>{file.displayName}</strong>
            <span className="modal-meta">{formatBytes(file.size)}</span>
          </div>
          <div className="modal-actions">
            <a href={url} download={file.displayName} className="icon-btn" title="Download">
              <Download size={20} />
            </a>
            <button className="icon-btn" onClick={onClose} title="Close">
              <X size={20} />
            </button>
          </div>
        </div>
        <div className="modal-body">{renderBody()}</div>
      </div>
    </div>
  );
}
