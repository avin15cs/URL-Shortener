import React from 'react';
import { Scissors } from 'lucide-react';

interface HeaderProps {
  urlCount: number;
}

const Header: React.FC<HeaderProps> = ({ urlCount }) => {
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
          <div className="header-badge">
            {urlCount > 0 ? `${urlCount} Links Active` : 'URL Shortener'}
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;
