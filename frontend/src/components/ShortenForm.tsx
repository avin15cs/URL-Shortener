import React, { useState, useRef } from 'react';
import toast from 'react-hot-toast';
import {
  Link2,
  Scissors,
  ChevronDown,
  Copy,
  Check,
  ExternalLink,
  Tag,
  Calendar,
  MousePointerClick,
} from 'lucide-react';
import { useShortenUrl } from '../hooks/useUrl';
import type { ShortenUrlRequest, UrlResponse } from '../types';

interface ShortenFormProps {
  onSuccess: (url: UrlResponse) => void;
  newUrl: UrlResponse | null;
}

const ShortenForm: React.FC<ShortenFormProps> = ({ onSuccess, newUrl }) => {
  const { shorten, loading } = useShortenUrl();
  const [formData, setFormData] = useState<ShortenUrlRequest>({
    originalUrl: '',
    customAlias: '',
    title: '',
  });
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [copied, setCopied] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);
  const urlInputRef = useRef<HTMLInputElement>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (validationError) setValidationError(null);
  };

  const validate = (): boolean => {
    const url = formData.originalUrl.trim();
    if (!url) {
      setValidationError('Please enter a URL to shorten');
      urlInputRef.current?.focus();
      return false;
    }
    if (!/^https?:\/\/.+/.test(url)) {
      setValidationError('URL must start with http:// or https://');
      urlInputRef.current?.focus();
      return false;
    }
    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const payload: ShortenUrlRequest = {
      originalUrl: formData.originalUrl.trim(),
    };
    if (formData.customAlias?.trim()) payload.customAlias = formData.customAlias.trim();
    if (formData.title?.trim()) payload.title = formData.title.trim();

    const result = await shorten(payload);
    if (result) {
      onSuccess(result);
      setFormData({ originalUrl: '', customAlias: '', title: '' });
      toast.success('URL shortened successfully! 🎉');
    } else {
      toast.error('Failed to shorten URL. Please try again.');
    }
  };

  const handleCopy = async () => {
    if (!newUrl) return;
    try {
      await navigator.clipboard.writeText(newUrl.shortUrl);
      setCopied(true);
      toast.success('Copied to clipboard!');
      setTimeout(() => setCopied(false), 2500);
    } catch {
      toast.error('Failed to copy');
    }
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  return (
    <div className="shorten-card">
      <h2 className="shorten-card-title">
        <span className="shorten-card-title-icon">✂️</span>
        Shorten a URL
      </h2>

      <form onSubmit={handleSubmit} noValidate>
        {/* Main URL input */}
        <div className="form-group">
          <label className="form-label" htmlFor="originalUrl">
            Long URL *
          </label>
          <div className="input-wrapper">
            <span className="input-icon">
              <Link2 size={16} />
            </span>
            <input
              ref={urlInputRef}
              id="originalUrl"
              name="originalUrl"
              type="url"
              className="form-input with-icon"
              placeholder="https://your-very-long-url.com/with/a/long/path?query=params"
              value={formData.originalUrl}
              onChange={handleChange}
              disabled={loading}
              autoComplete="url"
              autoFocus
            />
          </div>
          {validationError && (
            <div className="error-message">
              ⚠️ {validationError}
            </div>
          )}
        </div>

        {/* Advanced options toggle */}
        <button
          type="button"
          className="advanced-toggle"
          onClick={() => setShowAdvanced((v) => !v)}
          aria-expanded={showAdvanced}
          aria-controls="advanced-section"
        >
          <span className={`advanced-toggle-icon ${showAdvanced ? 'open' : ''}`}>
            <ChevronDown size={15} />
          </span>
          Advanced options {showAdvanced ? '(hide)' : '(show)'}
        </button>

        {/* Advanced section */}
        <div
          id="advanced-section"
          className={`advanced-section ${showAdvanced ? 'open' : ''}`}
          aria-hidden={!showAdvanced}
        >
          <div className="form-row">
            <div className="form-group">
              <label className="form-label" htmlFor="customAlias">
                Custom Alias
              </label>
              <div className="input-wrapper">
                <span className="input-icon">
                  <Tag size={15} />
                </span>
                <input
                  id="customAlias"
                  name="customAlias"
                  type="text"
                  className="form-input with-icon"
                  placeholder="my-brand-link"
                  value={formData.customAlias || ''}
                  onChange={handleChange}
                  disabled={loading}
                  maxLength={50}
                  pattern="^[a-zA-Z0-9_-]*$"
                />
              </div>
              <p className="form-hint">Letters, numbers, hyphens, underscores</p>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="title">
                Link Title
              </label>
              <div className="input-wrapper">
                <span className="input-icon">
                  <Calendar size={15} />
                </span>
                <input
                  id="title"
                  name="title"
                  type="text"
                  className="form-input with-icon"
                  placeholder="Optional display name"
                  value={formData.title || ''}
                  onChange={handleChange}
                  disabled={loading}
                  maxLength={255}
                />
              </div>
            </div>
          </div>
        </div>

        <button
          id="shorten-submit-btn"
          type="submit"
          className="btn-primary"
          disabled={loading}
          aria-label="Shorten URL"
        >
          {loading ? (
            <>
              <span className="spinner" aria-hidden="true" />
              Shortening...
            </>
          ) : (
            <>
              <Scissors size={17} />
              Shorten URL
            </>
          )}
        </button>
      </form>

      {/* Result */}
      {newUrl && (
        <div className="result-card" role="region" aria-label="Shortened URL result">
          <div className="result-label">
            <Check size={13} />
            Your Short URL is Ready
          </div>
          <div className="result-url-row">
            <a
              href={newUrl.shortUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="result-short-url"
              aria-label={`Short URL: ${newUrl.shortUrl}`}
            >
              {newUrl.shortUrl}
            </a>
            <button
              id="copy-result-btn"
              className={`copy-btn ${copied ? 'copied' : ''}`}
              onClick={handleCopy}
              aria-label="Copy short URL to clipboard"
            >
              {copied ? <Check size={14} /> : <Copy size={14} />}
              {copied ? 'Copied!' : 'Copy'}
            </button>
            <a
              href={newUrl.originalUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="btn-secondary"
              aria-label="Open original URL in new tab"
            >
              <ExternalLink size={14} />
              Visit
            </a>
          </div>
          <div className="result-meta">
            <span className="result-meta-item">
              <Link2 size={13} />
              <span>Original: <strong style={{ color: 'var(--color-text-secondary)', maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'inline-block', verticalAlign: 'bottom' }}>{newUrl.originalUrl}</strong></span>
            </span>
            <span className="result-meta-item">
              <MousePointerClick size={13} />
              <span>Clicks: <strong>{newUrl.clickCount}</strong></span>
            </span>
            <span className="result-meta-item">
              <Calendar size={13} />
              <span>Created: <strong>{formatDate(newUrl.createdAt)}</strong></span>
            </span>
          </div>
        </div>
      )}
    </div>
  );
};

export default ShortenForm;
