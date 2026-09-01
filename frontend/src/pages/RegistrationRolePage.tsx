import { Link } from "react-router-dom";

function TeacherIcon() {
  return (
    <svg viewBox="0 0 64 64" aria-hidden="true">
      <path d="M12 17.5 32 7l20 10.5L32 28 12 17.5Z" />
      <path d="M19 24v13c0 6 6 11 13 11s13-5 13-11V24" />
      <path d="M52 18v16" />
    </svg>
  );
}

function StudentIcon() {
  return (
    <svg viewBox="0 0 64 64" aria-hidden="true">
      <circle cx="32" cy="19" r="10" />
      <path d="M13 54c1-12 8-19 19-19s18 7 19 19" />
      <path d="M22 43h20v14H22z" />
    </svg>
  );
}

export function RegistrationRolePage() {
  return (
    <main className="role-page">
      <Link className="auth-back" to="/" aria-label="Back to home">
        <span aria-hidden="true">←</span> Back
      </Link>
      <Link className="brand role-page__brand" to="/">
        Classi<span>fy</span>
      </Link>
      <section className="role-picker" aria-labelledby="role-heading">
        <p className="eyebrow">Create your account</p>
        <h1 id="role-heading">How will you use Classify?</h1>
        <p className="role-picker__copy">
          Choose your account type to continue registration.
        </p>
        <div className="role-picker__grid">
          <Link className="role-card" to="/register/teacher">
            <span className="role-card__icon">
              <TeacherIcon />
            </span>
            <span className="role-card__title">I’m a Teacher</span>
            <span className="role-card__description">
              Create classes, teach subjects, and monitor student progress.
            </span>
            <span className="role-card__action">
              Register as Teacher <span aria-hidden="true">→</span>
            </span>
          </Link>
          <Link className="role-card" to="/register/student">
            <span className="role-card__icon">
              <StudentIcon />
            </span>
            <span className="role-card__title">I’m a Student</span>
            <span className="role-card__description">
              Join your class, access subjects, and follow your progress.
            </span>
            <span className="role-card__action">
              Register as Student <span aria-hidden="true">→</span>
            </span>
          </Link>
        </div>
        <p className="role-picker__login">
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </section>
    </main>
  );
}
