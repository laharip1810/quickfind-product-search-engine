/** Shared empty / error state block. */
export default function StateMessage({ tone = 'empty', title, children, action }) {
  return (
    <div className={`state state-${tone}`} role={tone === 'error' ? 'alert' : undefined}>
      <div className="state-icon" aria-hidden="true">{tone === 'error' ? '!' : '∅'}</div>
      <h3>{title}</h3>
      {children && <p>{children}</p>}
      {action}
    </div>
  );
}
