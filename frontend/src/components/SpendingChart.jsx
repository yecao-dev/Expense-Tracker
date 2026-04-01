import { useState, useEffect } from 'react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { getSpendingByCategory } from '../services/api';

const COLORS = [
  '#4F46E5', '#059669', '#D97706', '#DC2626', '#8B5CF6',
  '#EC4899', '#0891B2', '#65A30D', '#EA580C', '#6366F1'
];

export default function SpendingChart() {
  const [data, setData] = useState([]);

  useEffect(() => {
    const fetchData = async () => {
      const end = new Date().toISOString().split('T')[0];
      const start = new Date(Date.now() - 30 * 86400000)
        .toISOString().split('T')[0];

      try {
        const spending = await getSpendingByCategory(start, end);
        setData(spending.map(item => ({
          name: item.category,
          value: Math.abs(item.total)
        })));
      } catch (err) {
        console.error('Failed to fetch spending data:', err);
      }
    };
    fetchData();
  }, []);

  if (data.length === 0) return <p>No spending data yet. Connect a bank to get started.</p>;

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto' }}>
      <h2>Spending by Category</h2>
      <ResponsiveContainer width="100%" height={400}>
        <PieChart>
          <Pie
            data={data}
            cx="50%"
            cy="50%"
            outerRadius={150}
            dataKey="value"
            label={({ name, percent }) =>
              `${name} (${(percent * 100).toFixed(0)}%)`
            }
          >
            {data.map((entry, index) => (
              <Cell key={entry.name} fill={COLORS[index % COLORS.length]} />
            ))}
          </Pie>
          <Tooltip formatter={(value) => `$${value.toFixed(2)}`} />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}