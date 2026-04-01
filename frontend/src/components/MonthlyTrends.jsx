import { useState, useEffect } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { getMonthlyTrends } from '../services/api';

export default function MonthlyTrends() {
  const [data, setData] = useState([]);

  useEffect(() => {
    const fetchData = async () => {
      const end = new Date().toISOString().split('T')[0];
      const start = new Date(Date.now() - 365 * 86400000)
        .toISOString().split('T')[0];

      try {
        const trends = await getMonthlyTrends(start, end);
        setData(trends);
      } catch (err) {
        console.error('Failed to fetch trends:', err);
      }
    };
    fetchData();
  }, []);

  if (data.length === 0) return null;

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <h2>Monthly Spending Trends</h2>
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="month" />
          <YAxis />
          <Tooltip formatter={(value) => `$${value.toFixed(2)}`} />
          <Line
            type="monotone"
            dataKey="total"
            stroke="#4F46E5"
            strokeWidth={2}
            dot={{ fill: '#4F46E5' }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}