import { useCallback, useState } from 'react';
import { usePlaidLink } from 'react-plaid-link';
import { createLinkToken, exchangePublicToken, syncTransactions } from '../services/api';

export default function PlaidLinkButton({ onSuccess }) {
  const [linkToken, setLinkToken] = useState(null);
  const [loading, setLoading] = useState(false);

  const initializePlaid = async () => {
    setLoading(true);
    try {
      const token = await createLinkToken();
      setLinkToken(token);
    } catch (err) {
      console.error('Failed to create link token:', err);
    }
    setLoading(false);
  };

  const handleSuccess = useCallback(async (publicToken, metadata) => {
    console.log('Bank connected!', metadata.institution.name);
    await exchangePublicToken(publicToken);
    const result = await syncTransactions();
    console.log(`Synced ${result.new_transactions} new transactions`);
    if (onSuccess) onSuccess();
  }, [onSuccess]);

  const { open, ready } = usePlaidLink({
    token: linkToken,
    onSuccess: handleSuccess,
    onExit: (err) => {
      if (err) console.error('Plaid Link error:', err);
    }
  });

  return (
    <div>
      {!linkToken ? (
        <button
          onClick={initializePlaid}
          disabled={loading}
          style={{
            padding: '12px 24px',
            backgroundColor: '#4F46E5',
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            fontSize: '16px',
            cursor: loading ? 'not-allowed' : 'pointer'
          }}
        >
          {loading ? 'Setting up...' : '🏦 Connect Your Bank'}
        </button>
      ) : (
        <button
          onClick={() => open()}
          disabled={!ready}
          style={{
            padding: '12px 24px',
            backgroundColor: '#059669',
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            fontSize: '16px',
            cursor: ready ? 'pointer' : 'not-allowed'
          }}
        >
          {ready ? '🔗 Open Bank Login' : 'Loading...'}
        </button>
      )}
    </div>
  );
}