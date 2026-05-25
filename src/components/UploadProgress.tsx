import { CheckCircle, XCircle, Upload } from 'lucide-react';

interface UploadItem {
  id: string;
  name: string;
  progress: number;
  status: 'uploading' | 'done' | 'error';
}

interface Props {
  items: UploadItem[];
}

export function UploadProgress({ items }: Props) {
  return (
    <div className="upload-progress">
      <div className="upload-header">Uploads ({items.length})</div>
      <div className="upload-list">
        {items.map((item) => (
          <div key={item.id} className="upload-item">
            <div className="upload-item-row">
              {item.status === 'done' ? (
                <CheckCircle size={16} color="#34a853" />
              ) : item.status === 'error' ? (
                <XCircle size={16} color="#ea4335" />
              ) : (
                <Upload size={16} color="#4285f4" />
              )}
              <span className="upload-name">{item.name}</span>
              <span className="upload-pct">{Math.round(item.progress)}%</span>
            </div>
            <div className="upload-bar">
              <div
                className="upload-bar-fill"
                style={{
                  width: `${item.progress}%`,
                  background: item.status === 'error' ? '#ea4335' : '#4285f4',
                }}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
