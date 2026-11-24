import { useState } from 'react';

function formatJson(raw) {
  if (!raw) return '';
  try { return JSON.stringify(JSON.parse(raw), null, 2); }
  catch { return raw; }
}

export function JsonViewer({
  data,
  label,
  defaultCollapsed = false,
  maxHeight = 300
}) {
  const [collapsed, setCollapsed] = useState(defaultCollapsed);
  if (!data) return <span style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>none</span>;

  return (
    <div>
      {label && (
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
          <span className="label" style={{ marginBottom: 0 }}>{label}</span>
          <button
            type="button"
            className="btn btn-sm btn-secondary"
            onClick={() => setCollapsed(!collapsed)}
          >
            {collapsed ? 'expand' : 'collapse'}
          </button>
        </div>
      )}
      {!collapsed && (
        <div className="code-block" style={{ maxHeight }}>
          {formatJson(data)}
        </div>
      )}
    </div>
  );
}
