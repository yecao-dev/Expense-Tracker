import { useState, useEffect } from 'react';
import { getTransactions, exportCsv } from '../services/api';

export default function TransactionList() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      const end = new Date().toISOString().split('T')[0];
      const start = new Date(Date.now() - 30 * 86400000)
        .toISOString().split('T')[0];

      try {
        const data = await getTransactions(start, end);
        setTransactions(data);
      } catch (err) {
        console.error('Failed to fetch transactions:', err);
      }
      setLoading(false);
    };
    fetchData();
  }, []);

  if (loading) return <p>Loading transactions...</p>;

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>Recent Transactions</h2>
        <button onClick={() => {
          const end = new Date().toISOString().split('T')[0];
          const start = new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0];
          exportCsv(start, end);
        }} style={{
          padding: '8px 16px',
          backgroundColor: '#059669',
          color: 'white',
          border: 'none',
          borderRadius: '8px',
          cursor: 'pointer'
        }}>
          📥 Export CSV
        </button>
      </div>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #e5e7eb', textAlign: 'left' }}>
            <th style={{ padding: '12px 8px' }}>Date</th>
            <th style={{ padding: '12px 8px' }}>Merchant</th>
            <th style={{ padding: '12px 8px' }}>Category</th>
            <th style={{ padding: '12px 8px', textAlign: 'right' }}>Amount</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map(tx => (
            <tr key={tx.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
              <td style={{ padding: '12px 8px', color: '#6b7280' }}>{tx.date}</td>
              <td style={{ padding: '12px 8px', fontWeight: 500 }}>{tx.merchantName}</td>
              <td style={{ padding: '12px 8px' }}>
                <span style={{
                  backgroundColor: '#EEF2FF',
                  color: '#4F46E5',
                  padding: '4px 12px',
                  borderRadius: '16px',
                  fontSize: '13px'
                }}>
                  {tx.aiCategory}
                </span>
                {tx.aiConfidence && (
                  <span style={{ fontSize: '11px', color: '#9ca3af', marginLeft: '8px' }}>
                    {Math.round(tx.aiConfidence * 100)}%
                  </span>
                )}
              </td>
              <td style={{
                padding: '12px 8px',
                textAlign: 'right',
                fontFamily: 'monospace',
                color: tx.amount < 0 ? '#059669' : '#DC2626'
              }}>
                ${Math.abs(tx.amount).toFixed(2)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}