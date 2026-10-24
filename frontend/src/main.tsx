import React from 'react';
import ReactDOM from 'react-dom/client';
import { Toaster } from 'react-hot-toast';
import App from './App.tsx';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
    <Toaster
      position="top-right"
      toastOptions={{
        style: {
          background: '#18181b',
          color: '#fafafa',
          border: '1px solid rgba(255,255,255,0.1)',
          fontFamily: "'Inter', sans-serif",
          fontSize: '14px',
          fontWeight: 500,
          borderRadius: '12px',
          padding: '12px 16px',
        },
        success: {
          iconTheme: { primary: '#34d399', secondary: '#18181b' },
        },
        error: {
          iconTheme: { primary: '#f87171', secondary: '#18181b' },
        },
        duration: 4000,
      }}
    />
  </React.StrictMode>
);
