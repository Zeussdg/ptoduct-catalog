import { useEffect, useState } from "react";

export default function CategoryItem({
  category,
  counts,
  activeMain,
  activeSub,
  onSelectMain,
  onSelectSub,
}) {
  const isActiveGroup = activeMain === category.slug;
  const [expanded, setExpanded] = useState(isActiveGroup);

  useEffect(() => {
    if (isActiveGroup) setExpanded(true);
  }, [isActiveGroup]);

  const groupCount = category.subCategories.reduce(
    (sum, s) => sum + (counts[s.slug] || 0),
    0
  );
  if (groupCount === 0) return null;

  return (
    <div className="catg">
      <button
        type="button"
        className={"catg__main" + (isActiveGroup && !activeSub ? " catg__main--active" : "")}
        onClick={() => {
          onSelectMain(category.slug);
          setExpanded((e) => (isActiveGroup ? !e : true));
        }}
      >
        <span
          className={"catg__chevron" + (expanded ? " catg__chevron--open" : "")}
          aria-hidden="true"
        >
          ›
        </span>
        <span className="catg__main-label">{category.name}</span>
        <span className="catg__count">{groupCount}</span>
      </button>

      {expanded && (
        <ul className="catg__subs">
          {category.subCategories.map((s) => {
            const c = counts[s.slug] || 0;
            if (c === 0) return null;
            const active = isActiveGroup && activeSub === s.slug;
            return (
              <li key={s.slug}>
                <button
                  type="button"
                  className={"catg__sub" + (active ? " catg__sub--active" : "")}
                  onClick={() => onSelectSub(category.slug, s.slug)}
                >
                  <span>{s.name}</span>
                  <span className="catg__count">{c}</span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
