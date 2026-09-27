export default function ColorSwatches({ colors = [], max = 5 }) {
  const shown = colors.slice(0, max);
  return (
    <span className="swatches">
      {shown.map((c) => (
        <span key={c.id ?? c.name} className="swatch" style={{ background: c.hexCode }} title={c.name} aria-label={c.name} />
      ))}
      {colors.length > max && <span className="swatch-more">+{colors.length - max}</span>}
    </span>
  );
}
