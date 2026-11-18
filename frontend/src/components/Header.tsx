import React from 'react';
import { LogIn, LogOut, Scissors, ShieldCheck } from 'lucide-react';
import type { AuthSession } from '../types';

interface HeaderProps {
  urlCount: number;
  session: AuthSession | null;
  authLoading: boolean;
  onLogout: () => Promise<void>;
}

const Header: React.FC<HeaderProps> = ({ urlCount, session, authLoading, onLogout }) => {
  return (
    <header className="header">
      <div className="container">
        <div className="header-inner">
          <a href="/" className="logo" aria-label="Snip.ly Home">
            <div className="logo-icon">
              <Scissors size={18} color="#fff" strokeWidth={2.5} />
            </div>
            <span className="logo-text">
              Snip<span>.ly</span>
            </span>
          </a>
          <div className="header-actions">
            {session?.authenticated ? (
              <>
                <div className="header-identity">
                  {session.picture && <img src={session.picture} alt="" />}
                  <span>{session.name || session.email || 'Signed in'}</span>
                  {session.admin && (
                    <span className="admin-badge"><ShieldCheck size={12} /> URL Admin</span>
                  )}
                </div>
                <div className="header-badge">
                  {urlCount > 0 ? `${urlCount} Links Active` : 'No Active Links'}
                </div>
                <button className="header-auth-button" onClick={onLogout}>
                  <LogOut size={14} /> Sign out
                </button>
              </>
            ) : !authLoading ? (
              <a className="header-auth-button" href="/oauth2/authorization/auth-server">
                <LogIn size={14} /> Sign in
              </a>
            ) : null}
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;
