import { useState } from 'react';

interface Props {
  onSubmit: (name: string) => void;
  onClose: () => void;
}

export function NewFolderDialog({ onSubmit, onClose }: Props) {
  const [name, setName] = useState('');
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <h3>New folder</h3>
        <input
          autoFocus
          type="text"
          value={name}
          placeholder="Untitled folder"
          onChange={(e) => setName(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') onSubmit(name);
            if (e.key === 'Escape') onClose();
          }}
        />
        <div className="dialog-actions">
          <button onClick={onClose} className="btn-secondary">Cancel</button>
          <button onClick={() => onSubmit(name)} className="btn-primary" disabled={!name.trim()}>
            Create
          </button>
        </div>
      </div>
    </div>
  );
}
