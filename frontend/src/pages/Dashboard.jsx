import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { projectsApi } from '../api/projects';
import { FolderOpen, Plus, Zap, ArrowRight } from 'lucide-react';
import { format } from 'date-fns';

export default function Dashboard() {
  const { data: projects = [], isLoading } = useQuery({
    queryKey: ['projects'],
    queryFn: projectsApi.getAll,
  });

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <h1 style={{ fontSize: 26, fontWeight: 700, marginBottom: 4 }}>Dashboard</h1>
        <p style={{ color: 'var(--text-secondary)' }}>AI-powered API testing & contract intelligence agent</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <FolderOpen size={24} color="var(--accent-blue)" />
          <div>
            <div className="stat-value">{projects.length}</div>
            <div className="stat-label">Projects</div>
          </div>
        </div>
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <Zap size={24} color="var(--ai-purple-light)" />
          <div>
            <div className="stat-value" style={{ color: 'var(--ai-purple-light)' }}>AI</div>
            <div className="stat-label">Dynamic Test Scenarios</div>
          </div>
        </div>
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <Zap size={24} color="var(--accent-green)" />
          <div>
            <div className="stat-value" style={{ color: 'var(--accent-green)' }}>Contract</div>
            <div className="stat-label">OpenAPI 3.x Validation</div>
          </div>
        </div>
      </div>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h2 style={{ fontSize: 18, fontWeight: 600 }}>Active Projects</h2>
          <Link to="/projects/new" className="btn btn-primary btn-sm">
            <Plus size={14} /> New Project
          </Link>
        </div>

        {isLoading ? (
          <div style={{ textAlign: 'center', padding: 40 }}>
            <div className="spinner" style={{ width: 28, height: 28 }} />
          </div>
        ) : projects.length === 0 ? (
          <div className="empty-state">
            <p>No projects created yet.</p>
            <Link to="/projects/new" className="btn btn-primary">
              <Plus size={16} /> Create Project
            </Link>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {projects.map((p) => (
              <Link
                key={p.id}
                to={`/projects/${p.id}`}
                style={{ textDecoration: 'none' }}
              >
                <div
                  className="card card-sm"
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    cursor: 'pointer',
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 600, fontSize: 15 }}>{p.name}</div>
                    <div style={{ fontFamily: 'monospace', fontSize: 12, color: 'var(--accent-cyan)', marginTop: 2 }}>
                      {p.targetBaseUrl}
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                      {format(new Date(p.createdAt), 'MMM d, yyyy')}
                    </span>
                    <ArrowRight size={16} color="var(--text-muted)" />
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
