import { Link, useLocation } from 'react-router-dom';
import { LayoutDashboard, FolderOpen, Zap } from 'lucide-react';

export default function Layout({ children }) {
  const { pathname } = useLocation();

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="nav-brand">
          <Zap size={20} color="var(--accent-blue)" />
          <div>
            <div style={{ fontWeight: 700, fontSize: 15 }}>AI API Tester</div>
            <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>Contract Intelligence</div>
          </div>
        </div>

        <nav style={{ padding: '12px 0' }}>
          <Link
            to="/"
            className={`nav-item ${pathname === '/' ? 'active' : ''}`}
          >
            <LayoutDashboard size={16} />
            Dashboard
          </Link>
          <Link
            to="/projects"
            className={`nav-item ${pathname.startsWith('/projects') ? 'active' : ''}`}
          >
            <FolderOpen size={16} />
            Projects
          </Link>
        </nav>

        <div style={{ marginTop: 'auto', padding: 16, borderTop: '1px solid var(--border-color)', fontSize: 11, color: 'var(--text-muted)' }}>
          Powered by Spring AI + Ollama
        </div>
      </aside>

      <main className="main-content">{children}</main>
    </div>
  );
}
