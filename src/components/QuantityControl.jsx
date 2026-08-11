// Adet kontrolü: [- n +], minimum 1.
export default function QuantityControl({ value, onChange, disabled }) {
  const set = (v) => onChange(Math.max(1, v));
  return (
    <div className={"qqty" + (disabled ? " qqty--disabled" : "")}>
      <button
        type="button"
        onClick={() => set(value - 1)}
        disabled={disabled || value <= 1}
        aria-label="Azalt"
      >
        −
      </button>
      <input
        type="number"
        min="1"
        value={value}
        disabled={disabled}
        onChange={(e) => set(parseInt(e.target.value, 10) || 1)}
        aria-label="Adet"
      />
      <button type="button" onClick={() => set(value + 1)} disabled={disabled} aria-label="Artır">
        +
      </button>
    </div>
  );
}
