export function LearningIllustration({
  className = "",
}: {
  className?: string;
}) {
  return (
    <div className={`landing-art ${className}`} aria-hidden="true">
      <div className="landing-art__glow" />
      <div className="landing-art__card landing-art__card--one">
        <svg className="landing-art__book" viewBox="0 0 180 140">
          <path d="M18 29c29-8 52 0 72 18v75c-20-18-43-26-72-18V29Z" />
          <path d="M162 29c-29-8-52 0-72 18v75c20-18 43-26 72-18V29Z" />
          <path d="M90 47v75" />
          <path d="M32 48c17-2 31 2 43 10M32 65c17-2 31 2 43 10M148 48c-17-2-31 2-43 10M148 65c-17-2-31 2-43 10" />
        </svg>
      </div>
      <div className="landing-art__card landing-art__card--two">
        <svg className="landing-art__progress" viewBox="0 0 120 120">
          <circle cx="60" cy="60" r="40" />
          <path d="M60 20a40 40 0 1 1-38 53" />
          <path d="m44 60 11 11 23-25" />
        </svg>
      </div>
      <div className="landing-art__card landing-art__card--three">
        <svg viewBox="0 0 240 70">
          <path d="M22 50V33M58 50V20M94 50V39M130 50V12M166 50V27M202 50V17" />
          <path d="M12 54h212" />
        </svg>
      </div>
    </div>
  );
}
