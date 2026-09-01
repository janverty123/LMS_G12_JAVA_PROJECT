import { Link } from "react-router-dom";
import { ThemeToggle } from "@/components/shared/ThemeToggle";

export function LandingPage() {
  return (
    <div className="landing-page">
      <header className="landing-nav">
        <Link className="brand" to="/" aria-label="Classify home">Classi<span>fy</span></Link>
        <div className="landing-nav__actions">
          <ThemeToggle compact />
          <Link className="button button--outline" to="/login">LOGIN</Link>
        </div>
      </header>
      <main className="landing-hero">
        <section>
          <p className="eyebrow">Learning, clearly organized</p>
          <h1>One place for every class, subject, and milestone.</h1>
          <p className="landing-copy">Classify connects advisers, subject teachers, and students through a clear view of learning materials, requirements, grades, and progress.</p>
          <Link className="button button--primary" to="/register">GET STARTED →</Link>
        </section>
        <div className="landing-art" aria-hidden="true">
          <div className="landing-art__glow" />
          <div className="landing-art__card landing-art__card--one" />
          <div className="landing-art__card landing-art__card--two" />
          <div className="landing-art__card landing-art__card--three" />
        </div>
      </main>
    </div>
  );
}
