import { ChevronRight } from 'lucide-react';

interface Props {
  prefix: string;
  onNavigate: (prefix: string) => void;
}

export function Breadcrumbs({ prefix, onNavigate }: Props) {
  const parts = prefix.split('/').filter(Boolean);
  return (
    <div className="breadcrumbs">
      <button className="crumb" onClick={() => onNavigate('')}>
        My Drive
      </button>
      {parts.map((part, i) => {
        const newPrefix = parts.slice(0, i + 1).join('/') + '/';
        const isLast = i === parts.length - 1;
        return (
          <span key={i} className="crumb-wrap">
            <ChevronRight size={16} className="crumb-sep" />
            <button
              className={`crumb ${isLast ? 'crumb-current' : ''}`}
              onClick={() => onNavigate(newPrefix)}
            >
              {part}
            </button>
          </span>
        );
      })}
    </div>
  );
}
