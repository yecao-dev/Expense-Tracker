import { useState } from 'react';
import PlaidLinkButton from './PlaidLink';
import TransactionList from './TransactionList';
import SpendingChart from './SpendingChart';
import { logout, getUserName } from '../services/api';
import MonthlyTrends from './MonthlyTrends';
import BudgetAlerts from './BudgetAlerts';

export default function Dashboard({ onLogout }) {
  const [refreshKey, setRefreshKey] = useState(0);

  const handleLogout = () => {
    logout();
    onLogout();
  };

  return (
    <div style={{
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
      maxWidth: '1000px',
      margin: '0 auto',
      padding: '32px 16px'
    }}>
      <header style={{
        marginBottom: '32px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center'
      }}>
        <div>
          <h1 style={{ fontSize: '28px', fontWeight: 700, color: '#111827' }}>
            💰 Expense Tracker
          </h1>
          <p style={{ color: '#6b7280', marginTop: '4px' }}>
            Welcome, {getUserName()}
          </p>
        </div>
        <button onClick={handleLogout} style={{
          padding: '8px 16px',
          backgroundColor: '#f3f4f6',
          border: '1px solid #d1d5db',
          borderRadius: '8px',
          cursor: 'pointer'
        }}>
          Sign Out
        </button>
      </header>

      <section style={{ marginBottom: '40px' }}>
        <PlaidLinkButton onSuccess={() => setRefreshKey(k => k + 1)} />
      </section>

      <section style={{ marginBottom: '40px' }} key={`chart-${refreshKey}`}>
        <SpendingChart />
      </section>

      <section style={{ marginBottom: '40px' }} key={`trends-${refreshKey}`}>
        <MonthlyTrends />
      </section>

      <section style={{ marginBottom: '40px' }} key={`budgets-${refreshKey}`}>
        <BudgetAlerts />
      </section>

      <section key={`list-${refreshKey}`}>
        <TransactionList />
      </section>
    </div>
  );
}