import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, logout, isInstructor, isAdmin } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="brand">
          Learn<span className="brand-mark">Hub</span>
        </Link>

        <nav className="nav-links">
          <Link to="/courses">Browse courses</Link>

          {user && !isInstructor && <Link to="/dashboard">My learning</Link>}
          {user && isInstructor && <Link to="/instructor">Instructor studio</Link>}
          {user && isAdmin && <Link to="/admin">Admin</Link>}

          {!user && (
            <>
              <Link to="/login">Log in</Link>
              <Link to="/register" className="nav-cta">Get started</Link>
            </>
          )}

          {user && (
            <div className="nav-user-pill">
              <span>{user.name}</span>
              <button className="link-btn" onClick={handleLogout}>Log out</button>
            </div>
          )}
        </nav>
      </div>
    </header>
  );
}
