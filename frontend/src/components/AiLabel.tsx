export function AiLabel({ text = 'AI Analysis' }: { text?: string }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 11, fontWeight: 600, color: 'var(--ai-purple-light)', textTransform: 'uppercase' }}>
      ✦ {text}
    </div>
  );
}

export function AiBadge() {
  return <span className="badge badge-ai">✦ AI Generated</span>;
}
