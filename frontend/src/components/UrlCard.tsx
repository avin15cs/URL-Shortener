import React, { useState } from 'react';
import {
  Copy,
  Check,
  ExternalLink,
  Trash2,
  MousePointerClick,
  Calendar,
  Globe,
} from 'lucide-react';
import toast from 'react-hot-toast';
import type { UrlResponse } from '../types';

interface UrlCardProps {
  url: UrlResponse;
  onDelete: (id: number) => Promise<void>;
}

const getFaviconUrl = (originalUrl: string): string => {
  try {
    const domain = new URL(originalUrl).hostname;
    return `https://www.google.com/s2/favicons?domain=${domain}&sz=32`;
  } catch {
    return '';
  }
};

const getDomain = (originalUrl: string): string => {
  try {
    return new URL(originalUrl).hostname;
  } catch {
    return originalUrl;
  }
};

const formatDate = (dateStr: string): string => {
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
};

const UrlCard: React.FC<UrlCardProps> = ({ url, onDelete }) => {
  const [copied, setCopied] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [imgError, setImgError] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(url.shortUrl);
      setCopied(true);
      toast.success('Copied to clipboard!');
      setTimeout(() => setCopied(false), 2500);
    } catch {
      toast.error('Failed to copy');
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Delete this short link? This action cannot be undone.')) return;
    setDeleting(true);
    try {
      await onDelete(url.id);
    } finally {
      setDeleting(false);
    }
  };

  const faviconUrl = getFaviconUrl(url.originalUrl);
  const domain = getDomain(url.originalUrl);

  return (
    <article className="url-card" role="listitem" aria-label={`Short link: ${url.shortUrl}`}>
      {/* Favicon */}
      <div className="url-card-favicon" aria-hidden="true">
        {faviconUrl && !imgError ? (
          <img
            src={faviconUrl}
            alt={`${domain} favicon`}
            width={20}
            height={20}
            onError={() => setImgError(true)}
          />
        ) : (
          <Globe size={18} />
        )}
      </div>

      {/* Body */}
      <div className="url-card-body">
        <div className="url-card-short">
          <a
            href={url.shortUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="url-card-short-link"
            aria-label={`Short URL: ${url.shortUrl}`}
          >
            {url.shortUrl.replace(/^https?:\/\//, '')}
          </a>
          {url.title && (
            <span
              style={{
                fontSize: 12,
                color: 'var(--color-text-muted)',
                background: 'rgba(255,255,255,0.04)',
                border: '1px solid rgba(255,255,255,0.08)',
                borderRadius: 'var(--radius-full)',
                padding: '2px 8px',
                whiteSpace: 'nowrap',
              }}
            >
              {url.title}
            </span>
          )}
        </div>

        <p className="url-card-original" title={url.originalUrl}>
          {url.originalUrl}
        </p>

        <div className="url-card-meta">
          <span className="click-badge" aria-label={`${url.clickCount} clicks`}>
            <MousePointerClick size={11} />
            {url.clickCount} {url.clickCount === 1 ? 'click' : 'clicks'}
          </span>
          <span className="url-card-meta-item">
            <Calendar size={12} />
            {formatDate(url.createdAt)}
          </span>
          <span className="url-card-meta-item">
            <Globe size={12} />
            {domain}
          </span>
          {url.customAlias && (
            <span className="url-card-meta-item" style={{ color: 'var(--color-violet)' }}>
              alias: {url.customAlias}
            </span>
          )}
        </div>
      </div>

      {/* Actions */}
      <div className="url-card-actions">
        <button
          id={`copy-btn-${url.id}`}
          className={`copy-btn ${copied ? 'copied' : ''}`}
          onClick={handleCopy}
          aria-label="Copy short URL"
          title="Copy short URL"
        >
          {copied ? <Check size={14} /> : <Copy size={14} />}
          {copied ? 'Copied' : 'Copy'}
        </button>

        <a
          href={url.originalUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="btn-secondary"
          aria-label="Open original URL"
          title="Open original URL"
        >
          <ExternalLink size={14} />
        </a>

        <button
          id={`delete-btn-${url.id}`}
          className="btn-danger"
          onClick={handleDelete}
          disabled={deleting}
          aria-label="Delete this short link"
          title="Delete link"
        >
          {deleting ? (
            <span className="spinner" style={{ width: 14, height: 14, borderWidth: 1.5 }} />
          ) : (
            <Trash2 size={14} />
          )}
        </button>
      </div>
    </article>
  );
};

export default UrlCard;
