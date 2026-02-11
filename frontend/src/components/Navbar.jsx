import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function Navbar() {
  const { role, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="nav">
      <div className="brand">
        FinNexus
        <span className="live-badge"><span className="live-dot" />LIVE</span>
      </div>
      <div className="nav-links">
        <Link to="/dashboard">Dashboard</Link>
        <Link to="/trade">Trade</Link>
        <Link to="/orders">Orders</Link>
        <Link to="/wallet">Wallet</Link>
        <Link to="/strategies">Robo Strategies</Link>
        <Link to="/analytics">Analytics</Link>
        {role === 'ADMIN' && <Link to="/admin">Admin</Link>}
      </div>
      <button className="btn outline" onClick={handleLogout}>Logout</button>
    </nav>
  );
}
