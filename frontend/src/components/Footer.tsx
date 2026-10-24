import React from 'react';
import { Scissors, Github, Heart } from 'lucide-react';

const Footer: React.FC = () => {
  const year = new Date().getFullYear();

  return (
    <footer className="footer">
      <div className="container">
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 8,
            marginBottom: 8,
          }}
        >
          <Scissors size={14} style={{ color: 'var(--color-primary-light)' }} />
          <span style={{ color: 'var(--color-text-secondary)', fontWeight: 600 }}>
            Snip.ly
          </span>
        </div>
        <p>
          © {year} Snip.ly — Built with{' '}
          <Heart
            size={12}
            style={{ display: 'inline', verticalAlign: 'middle', color: 'var(--color-pink)' }}
            fill="currentColor"
          />{' '}
          using React &amp; Spring Boot
        </p>
      </div>
    </footer>
  );
};

export default Footer;
