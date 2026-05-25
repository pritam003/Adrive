import { Search, LayoutGrid, List } from 'lucide-react';
import type { ViewMode } from '../types';

interface Props {
  search: string;
  setSearch: (s: string) => void;
  view: ViewMode;
  setView: (v: ViewMode) => void;
}

export function Topbar({ search, setSearch, view, setView }: Props) {
  return (
    <div className="topbar">
      <div className="search">
        <Search size={18} className="search-icon" />
        <input
          type="text"
          placeholder="Search in Drive"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>
      <div className="view-toggle">
        <button
          className={view === 'list' ? 'active' : ''}
          onClick={() => setView('list')}
          title="List view"
        >
          <List size={18} />
        </button>
        <button
          className={view === 'grid' ? 'active' : ''}
          onClick={() => setView('grid')}
          title="Grid view"
        >
          <LayoutGrid size={18} />
        </button>
      </div>
    </div>
  );
}
