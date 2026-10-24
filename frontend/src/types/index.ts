export interface UrlResponse {
  id: number;
  originalUrl: string;
  shortCode: string;
  shortUrl: string;
  customAlias: string | null;
  title: string | null;
  clickCount: number;
  createdAt: string;
  updatedAt: string;
  expiresAt: string | null;
  isActive: boolean;
}

export interface ShortenUrlRequest {
  originalUrl: string;
  customAlias?: string;
  title?: string;
  expiresAt?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}
