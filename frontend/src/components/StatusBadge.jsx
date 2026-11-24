export function StatusBadge({ status }) {
  const cls = `badge badge-${status.toLowerCase().replace(/_/g, '-')}`;
  return <span className={cls}>{status}</span>;
}

export function MethodBadge({ method }) {
  return (
    <span className={`method-badge method-${method.toLowerCase()}`}>
      {method.toUpperCase()}
    </span>
  );
}
