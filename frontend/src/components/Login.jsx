import { useState } from 'react';
import { login, register } from '../services/api';

export default function Login({ onLogin }) {
  const [isRegister, setIsRegister] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (isRegister) {
        await register(name, email, password);
      } else {
        await login(email, password);
      }
      onLogin();
    } catch (err) {
      setError(err.response?.data?.error || 'Something went wrong');
    }
  };

  return (
    <div style={{
      maxWidth: '400px',
      margin: '100px auto',
      padding: '32px',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif'
    }}>
      <h1 style={{ fontSize: '28px', fontWeight: 700, marginBottom: '8px' }}>
        💰 Expense Tracker
      </h1>
      <p style={{ color: '#6b7280', marginBottom: '24px' }}>
        {isRegister ? 'Create an account' : 'Sign in to your account'}
      </p>

      {error && (
        <p style={{ color: '#DC2626', marginBottom: '16px' }}>{error}</p>
      )}

      <form onSubmit={handleSubmit}>
        {isRegister && (
          <input
            type="text"
            placeholder="Full Name"
            value={name}
            onChange={e => setName(e.target.value)}
            required
            style={inputStyle}
          />
        )}
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={e => setEmail(e.target.value)}
          required
          style={inputStyle}
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={e => setPassword(e.target.value)}
          required
          style={inputStyle}
        />
        <button type="submit" style={{
          width: '100%',
          padding: '12px',
          backgroundColor: '#4F46E5',
          color: 'white',
          border: 'none',
          borderRadius: '8px',
          fontSize: '16px',
          cursor: 'pointer',
          marginTop: '8px'
        }}>
          {isRegister ? 'Sign Up' : 'Sign In'}
        </button>
      </form>

      <p style={{ marginTop: '16px', textAlign: 'center', color: '#6b7280' }}>
        {isRegister ? 'Already have an account?' : "Don't have an account?"}{' '}
        <span
          onClick={() => setIsRegister(!isRegister)}
          style={{ color: '#4F46E5', cursor: 'pointer' }}
        >
          {isRegister ? 'Sign In' : 'Sign Up'}
        </span>
      </p>
    </div>
  );
}

const inputStyle = {
  width: '100%',
  padding: '12px',
  marginBottom: '12px',
  border: '1px solid #d1d5db',
  borderRadius: '8px',
  fontSize: '14px',
  boxSizing: 'border-box'
};