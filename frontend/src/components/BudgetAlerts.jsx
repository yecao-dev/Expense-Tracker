import { useState, useEffect } from 'react';
import { getBudgets, createBudget, deleteBudget } from '../services/api';

const CATEGORIES = [
  'Groceries', 'Dining Out', 'Coffee & Cafes', 'Transportation',
  'Rideshare', 'Gas & Fuel', 'Rent & Housing', 'Utilities',
  'Phone & Internet', 'Health & Pharmacy', 'Insurance',
  'Subscriptions', 'Online Shopping', 'In-Store Shopping',
  'Entertainment', 'Travel & Hotels', 'Education',
  'Fitness & Gym', 'Personal Care', 'Gifts & Donations',
  'Income', 'Transfer', 'Fees & Charges', 'Other'
];

export default function BudgetAlerts() {
  const [budgets, setBudgets] = useState([]);
  const [category, setCategory] = useState(CATEGORIES[0]);
  const [limit, setLimit] = useState('');
  const [showForm, setShowForm] = useState(false);

  const fetchBudgets = async () => {
    try {
      const data = await getBudgets();
      setBudgets(data);
    } catch (err) {
      console.error('Failed to fetch budgets:', err);
    }
  };

  useEffect(() => { fetchBudgets(); }, []);

  const handleAdd = async () => {
    if (!limit) return;
    await createBudget(category, limit);
    setLimit('');
    setShowForm(false);
    fetchBudgets();
  };

  const handleDelete = async (id) => {
    await deleteBudget(id);
    fetchBudgets();
  };

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>Budget Alerts</h2>
        <button onClick={() => setShowForm(!showForm)} style={{
          padding: '8px 16px',
          backgroundColor: '#4F46E5',
          color: 'white',
          border: 'none',
          borderRadius: '8px',
          cursor: 'pointer'
        }}>
          {showForm ? 'Cancel' : '+ Add Budget'}
        </button>
      </div>

      {showForm && (
        <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
          <select value={category} onChange={e => setCategory(e.target.value)}
            style={{ padding: '8px', borderRadius: '8px', border: '1px solid #d1d5db' }}>
            {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
          </select>
          <input
            type="number"
            placeholder="Monthly limit ($)"
            value={limit}
            onChange={e => setLimit(e.target.value)}
            style={{ padding: '8px', borderRadius: '8px', border: '1px solid #d1d5db', width: '150px' }}
          />
          <button onClick={handleAdd} style={{
            padding: '8px 16px',
            backgroundColor: '#059669',
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            cursor: 'pointer'
          }}>
            Save
          </button>
        </div>
      )}

      {budgets.length === 0 ? (
        <p style={{ color: '#6b7280' }}>No budgets set. Add one to track spending limits.</p>
      ) : (
        budgets.map(b => (
          <div key={b.id} style={{
            padding: '16px',
            marginBottom: '12px',
            borderRadius: '8px',
            border: b.overBudget ? '2px solid #DC2626' : '1px solid #e5e7eb',
            backgroundColor: b.overBudget ? '#FEF2F2' : 'white'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
              <strong>{b.category}</strong>
              <span>
                ${b.spent.toFixed(2)} / ${b.monthlyLimit.toFixed(2)}
                {b.overBudget && <span style={{ color: '#DC2626', marginLeft: '8px' }}>⚠️ Over budget!</span>}
              </span>
            </div>
            <div style={{ backgroundColor: '#e5e7eb', borderRadius: '4px', height: '8px' }}>
              <div style={{
                width: `${Math.min(b.percentage, 100)}%`,
                height: '100%',
                borderRadius: '4px',
                backgroundColor: b.percentage > 100 ? '#DC2626' : b.percentage > 80 ? '#D97706' : '#059669'
              }} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '4px' }}>
              <span style={{ fontSize: '12px', color: '#6b7280' }}>{Math.round(b.percentage)}% used</span>
              <button onClick={() => handleDelete(b.id)}
                style={{ fontSize: '12px', color: '#DC2626', background: 'none', border: 'none', cursor: 'pointer' }}>
                Remove
              </button>
            </div>
          </div>
        ))
      )}
    </div>
  );
}