import React, { useState, useEffect } from 'react';
import Header from './components/Header';
import ShortenForm from './components/ShortenForm';
import UrlList from './components/UrlList';
import Footer from './components/Footer';
import { useUrlList } from './hooks/useUrl';
import type { UrlResponse } from './types';

const App: React.FC = () => {
  const { urls, loading, error, refresh, deleteUrl } = useUrlList();
  const [newUrl, setNewUrl] = useState<UrlResponse | null>(null);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const handleUrlShortened = (url: UrlResponse) => {
    setNewUrl(url);
    refresh();
  };

  return (
    <div className="app-layout">
      <Header urlCount={urls.length} />
      <main>
        <div className="container">
          <section className="hero">
            <div className="hero-eyebrow">
              <span className="hero-eyebrow-dot" />
              Fast · Reliable · Open Source
            </div>
            <h1 className="hero-title">
              Shorten Links,{' '}
              <span className="hero-title-accent">Amplify Reach</span>
            </h1>
            <p className="hero-subtitle">
              Transform any long URL into a sleek, shareable short link in seconds.
              Track clicks, manage aliases, and own your links.
            </p>
          </section>

          <ShortenForm onSuccess={handleUrlShortened} newUrl={newUrl} />

          <UrlList
            urls={urls}
            loading={loading}
            error={error}
            onDelete={deleteUrl}
            onRefresh={refresh}
          />
        </div>
      </main>
      <Footer />
    </div>
  );
};

export default App;
