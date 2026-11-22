import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { projectsApi } from '../api/projects';
import { Plus, FolderOpen, ArrowRight } from 'lucide-react';
import { format } from 'date-fns';

export default function ProjectList() {
  const { data: projects = [], isLoading } = useQuery({
    queryKey: ['projects'],
    queryFn: projectsApi.getAll,
  });

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <h1 style={{ fontSize: 26, fontWeight: 700, marginBottom: 4 }}>Projects</h1>
          <p style={{ color: 'var(--text-secondary)' }}>Configure target APIs and OpenAPI specifications</p>
        </div>
        <Link to="/projects/new" className="btn btn-primary">
          <Plus size={16} /> New Project
        </Link>
      </div>

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: 60 }}>
          <div className="spinner" style={{ width: 32, height: 32 }} />
        </div>
      ) : projects.length === 0 ? (
        <div className="empty-state card">
          <p>No projects configured yet.</p>
          <Link to="/projects/new" className="btn btn-primary">
            <Plus size={16} /> Create Project
          </Link>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: 16 }}>
          {projects.map((p) => (
            <Link key={p.id} to={`/projects/${p.id}`} style={{ textDecoration: 'none' }}>
              <div className="card" style={{ cursor: 'pointer' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <FolderOpen size={16} color="var(--accent-blue)" />
                    <span style={{ fontWeight: 600 }}>{p.name}</span>
                  </div>
                  <ArrowRight size={14} color="var(--text-muted)" />
                </div>
                {p.description && (
                  <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 10 }}>
                    {p.description}
                  </p>
                )}
                <div style={{
                  fontFamily: 'monospace',
                  fontSize: 12,
                  color: 'var(--accent-cyan)',
                  background: 'var(--bg-secondary)',
                  padding: '6px 10px',
                  borderRadius: 4,
                  marginBottom: 10
                }}>
                  {p.targetBaseUrl}
                </div>
                <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                  Created {format(new Date(p.createdAt), 'MMM d, yyyy')}
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
