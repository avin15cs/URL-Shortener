import React, { useState, useCallback } from 'react';
import { urlApi } from '../api/urlApi';
import type { ShortenUrlRequest, UrlResponse } from '../types';

interface UseShortenUrlReturn {
  shorten: (request: ShortenUrlRequest) => Promise<UrlResponse | null>;
  loading: boolean;
  error: string | null;
}

export const useShortenUrl = (): UseShortenUrlReturn => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const shorten = useCallback(async (request: ShortenUrlRequest): Promise<UrlResponse | null> => {
    setLoading(true);
    setError(null);
    try {
      const response = await urlApi.shorten(request);
      if (response.success) {
        return response.data;
      }
      setError(response.message);
      return null;
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to shorten URL. Please try again.';
      setError(message);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  return { shorten, loading, error };
};

interface UseUrlListReturn {
  urls: UrlResponse[];
  loading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
  deleteUrl: (id: number) => Promise<void>;
}

export const useUrlList = (): UseUrlListReturn => {
  const [urls, setUrls] = useState<UrlResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await urlApi.getAll();
      if (response.success) {
        setUrls(response.data);
      }
    } catch (err: unknown) {
      setError('Failed to load URLs');
    } finally {
      setLoading(false);
    }
  }, []);

  const deleteUrl = useCallback(
    async (id: number) => {
      try {
        await urlApi.delete(id);
        setUrls((prev) => prev.filter((u) => u.id !== id));
      } catch {
        throw new Error('Failed to delete URL');
      }
    },
    []
  );

  return { urls, loading, error, refresh, deleteUrl };
};

// React import used for JSX compatibility
void React;
