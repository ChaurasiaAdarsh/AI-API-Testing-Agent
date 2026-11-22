export function StatusBadge({ status }: { status: string }) {
  const cls = `badge badge-${status.toLowerCase().replace(/_/g, '-')}`;
  return <span className={cls}>{status}</span>;
}

export function MethodBadge({ method }: { method: string }) {
  return (
    <span className={`method-badge method-${method.toLowerCase()}`}>
      {method.toUpperCase()}
    </span>
  );
}
