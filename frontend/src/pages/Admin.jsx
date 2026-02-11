import React, { useEffect, useState } from 'react';
import api from '../api/axios.js';

export default function Admin() {
  const [users, setUsers] = useState([]);
  const [stats, setStats] = useState(null);

  const load = async () => {
    const [usersRes, statsRes] = await Promise.all([
      api.get('/admin/users'),
      api.get('/admin/stats')
    ]);
    setUsers(usersRes.data);
    setStats(statsRes.data);
  };

  useEffect(() => {
    load();
  }, []);

  const toggle = async (user) => {
    if (user.enabled) {
      await api.post(`/admin/users/${user.id}/disable`);
    } else {
      await api.post(`/admin/users/${user.id}/enable`);
    }
    load();
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2>Admin Dashboard</h2>
          <p className="muted">Monitor platform activity and users.</p>
        </div>
      </div>

      <div className="grid stats">
        <div className="card stat-card">
          <div className="stat-label">Total Users</div>
          <div className="stat-value">{stats ? stats.totalUsers : '--'}</div>
        </div>
        <div className="card stat-card">
          <div className="stat-label">Total Orders</div>
          <div className="stat-value">{stats ? stats.totalOrders : '--'}</div>
        </div>
        <div className="card stat-card">
          <div className="stat-label">Total Trades</div>
          <div className="stat-value">{stats ? stats.totalTrades : '--'}</div>
        </div>
        <div className="card stat-card">
          <div className="stat-label">Open Trades</div>
          <div className="stat-value">{stats ? stats.openTrades : '--'}</div>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <h3>Users</h3>
        </div>
        <div className="table">
          <div className="table-row header cols-5">
            <span>Username</span>
            <span>Email</span>
            <span>Role</span>
            <span>Status</span>
            <span>Action</span>
          </div>
          {users.map((user) => (
            <div key={user.id} className="table-row cols-5">
              <span>{user.username}</span>
              <span>{user.email}</span>
              <span>{user.role}</span>
              <span>{user.enabled ? 'Enabled' : 'Disabled'}</span>
              <button className="btn outline" onClick={() => toggle(user)}>
                {user.enabled ? 'Disable' : 'Enable'}
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
