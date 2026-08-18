import React from 'react';

export default function Footer() {
  return (
    <footer className="footer">
      <div className="container">
        LearnHub — teach what you know, learn what you don't. © {new Date().getFullYear()}
      </div>
    </footer>
  );
}
