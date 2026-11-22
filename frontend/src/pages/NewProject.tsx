import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { projectsApi } from '../api/projects';
import { ArrowLeft, Zap } from 'lucide-react';

export default function NewProject() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [form, setForm] = useState({ name: '', description: '', targetBaseUrl: 'http://localhost:8080' });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const mutation = useMutation({
    mutationFn: projectsApi.create,
    onSuccess: (project) => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      navigate(`/projects/${project.id}`);
    },
  });

  const validate = () => {
    const errs: Record<string, string> = {};
    if (!form.name.trim()) errs.name = 'Project name is required';
    if (!form.targetBaseUrl.trim()) errs.targetBaseUrl = 'Target base URL is required';
    else if (!/^https?:\/\/.+/.test(form.targetBaseUrl))
      errs.targetBaseUrl = 'URL must start with http:// or https://';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (validate()) mutation.mutate(form);
  };

  return (
    <div style={{ maxWidth: 600, margin: '0 auto' }}>
      <Link to="/projects" className="btn btn-secondary btn-sm" style={{ display: 'inline-flex', marginBottom: 16 }}>
        <ArrowLeft size={14} /> Back to Projects
      </Link>

      <h1 style={{ fontSize: 26, fontWeight: 700, marginBottom: 4 }}>New Project</h1>
      <p style={{ color: 'var(--text-secondary)', marginBottom: 20 }}>Configure target API connection details</p>

      <div className="card">
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="label">Project Name *</label>
            <input
              className="input"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="e.g. User Management Service"
              autoFocus
            />
            {errors.name && <div className="error-msg">{errors.name}</div>}
          </div>

          <div className="form-group">
            <label className="label">Description</label>
            <textarea
              className="textarea"
              rows={3}
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              placeholder="Optional overview of the API under test"
            />
          </div>

          <div className="form-group">
            <label className="label">Target API Base URL *</label>
            <input
              className="input"
              value={form.targetBaseUrl}
              onChange={(e) => setForm({ ...form, targetBaseUrl: e.target.value })}
              placeholder="http://localhost:8080"
            />
            {errors.targetBaseUrl && <div className="error-msg">{errors.targetBaseUrl}</div>}
            <div className="hint">
              Target API base address. For the built-in test suite, use <code>http://localhost:8080</code>
            </div>
          </div>

          {mutation.isError && (
            <div className="error-msg" style={{ marginBottom: 16 }}>
              {(mutation.error as Error).message}
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', marginTop: 8 }}
            disabled={mutation.isPending}
          >
            {mutation.isPending ? (
              <>
                <div className="spinner" /> Creating...
              </>
            ) : (
              <>
                <Zap size={16} /> Create Project
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
}
