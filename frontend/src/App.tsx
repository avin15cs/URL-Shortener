import React, { useState, useEffect } from 'react';
import Header from './components/Header';
import ShortenForm from './components/ShortenForm';
import UrlList from './components/UrlList';
import Footer from './components/Footer';
import { useUrlList } from './hooks/useUrl';
import { authApi } from './api/urlApi';
import type { AuthSession, UrlResponse } from './types';

const App: React.FC = () => {
  const { urls, loading, error, refresh, deleteUrl } = useUrlList();
  const [newUrl, setNewUrl] = useState<UrlResponse | null>(null);
  const [session, setSession] = useState<AuthSession | null>(null);
  const [authLoading, setAuthLoading] = useState(true);

  useEffect(() => {
    authApi.session()
      .then((currentSession) => {
        setSession(currentSession);
        if (currentSession.authenticated) {
          refresh();
        }
      })
      .finally(() => setAuthLoading(false));
  }, [refresh]);

  const handleLogout = async () => {
    await authApi.logout();
    window.location.assign('/');
  };

  const handleUrlShortened = (url: UrlResponse) => {
    setNewUrl(url);
    refresh();
  };

  return (
    <div className="app-layout">
      <Header
        urlCount={urls.length}
        session={session}
        authLoading={authLoading}
        onLogout={handleLogout}
      />
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

          {!authLoading && !session?.authenticated ? (
            <section className="auth-card" aria-label="Sign in required">
              <h2>Sign in to manage your links</h2>
              <p>
                Continue with Google through the authorization server. Your links are private
                to your account unless you have the URL administrator role.
              </p>
              <a className="btn-primary auth-login" href="/oauth2/authorization/auth-server">
                Sign in with Google
              </a>
            </section>
          ) : session?.authenticated ? (
            <>
              <ShortenForm onSuccess={handleUrlShortened} newUrl={newUrl} />

              <UrlList
                urls={urls}
                loading={loading}
                error={error}
                isAdmin={session.admin}
                onDelete={deleteUrl}
                onRefresh={refresh}
              />
            </>
          ) : null}
        </div>
      </main>
      <Footer />
    </div>
  );
};

export default App;
