import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: {
    template: '%s | Blog',
    default: 'Blog',
  },
  description: 'A Next.js 14 blog application',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <div className="app-container">
          <header className="app-header">
            <nav className="nav">
              <a href="/" className="nav-logo">
                Blog
              </a>
              <div className="nav-links">
                <a href="/posts" className="nav-link">
                  Posts
                </a>
                <a href="/posts/new" className="nav-link">
                  New Post
                </a>
              </div>
            </nav>
          </header>
          {children}
          <footer className="app-footer">
            <p>&copy; {new Date().getFullYear()} Blog. All rights reserved.</p>
          </footer>
        </div>
      </body>
    </html>
  );
}
