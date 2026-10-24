import React from 'react';
import toast from 'react-hot-toast';
import {
  Link2,
  Copy,
  ExternalLink,
  Trash2,
  MousePointerClick,
  RefreshCw,
  Globe,
} from 'lucide-react';
import UrlCard from './UrlCard';
import type { UrlResponse } from '../types';

interface UrlListProps {
  urls: UrlResponse[];
  loading: boolean;
  error: string | null;
  onDelete: (id: number) => Promise<void>;
  onRefresh: () => Promise<void>;
}

const SkeletonCard: React.FC = () => (
  <div className="skeleton-card">
    <div className="skeleton" style={{ width: 40, height: 40, borderRadius: 8, flexShrink: 0 }} />
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div className="skeleton" style={{ width: '40%', height: 16 }} />
      <div className="skeleton" style={{ width: '70%', height: 12 }} />
      <div className="skeleton" style={{ width: '30%', height: 12 }} />
    </div>
  </div>
);

const UrlList: React.FC<UrlListProps> = ({
  urls,
  loading,
  error,
  onDelete,
  onRefresh,
}) => {
  const handleDelete = async (id: number) => {
    try {
      await onDelete(id);
      toast.success('URL deleted successfully');
    } catch {
      toast.error('Failed to delete URL');
    }
  };

  return (
    <section aria-label="Your shortened URLs" style={{ paddingBottom: 60 }}>
      <div className="section-header">
        <h2 className="section-title">
          <Link2 size={20} />
          Your Links
          {urls.length > 0 && (
            <span className="section-count" aria-label={`${urls.length} links`}>
              {urls.length}
            </span>
          )}
        </h2>
        <button
          id="refresh-urls-btn"
          className="btn-secondary"
          onClick={onRefresh}
          disabled={loading}
          aria-label="Refresh URL list"
        >
          <RefreshCw size={14} className={loading ? 'spin-slow' : ''} />
          Refresh
        </button>
      </div>

      {error && (
        <div className="error-message" role="alert">
          ⚠️ {error}
        </div>
      )}

      {loading && urls.length === 0 ? (
        <div className="url-list" aria-label="Loading URLs">
          {Array.from({ length: 3 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : urls.length === 0 ? (
        <div className="empty-state" role="status">
          <div className="empty-state-icon">
            <Globe size={64} opacity={0.3} />
          </div>
          <p className="empty-state-title">No links yet</p>
          <p className="empty-state-subtitle">
            Shorten your first URL above and it will appear here with click tracking.
          </p>
        </div>
      ) : (
        <div className="url-list" role="list">
          {urls.map((url) => (
            <UrlCard key={url.id} url={url} onDelete={handleDelete} />
          ))}
        </div>
      )}
    </section>
  );
};

export default UrlList;
