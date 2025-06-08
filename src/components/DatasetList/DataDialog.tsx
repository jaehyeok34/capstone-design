import React from 'react';

interface DataDialogProps {
  open: boolean;
  onClose: () => void;
  datasetInfo: string | null;
  data: Record<string, any[]> | null;
  loading: boolean;
  error: string | null;
}

const dialogStyle: React.CSSProperties = {
  position: 'fixed',
  top: 0,
  left: 0,
  width: '100vw',
  height: '100vh',
  background: 'rgba(0,0,0,0.35)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  zIndex: 1000,
};

const modalStyle: React.CSSProperties = {
  background: '#23272f',
  borderRadius: 12,
  padding: '2rem',
  minWidth: 400,
  maxWidth: '90vw',
  maxHeight: '80vh',
  overflow: 'auto',
  boxShadow: '0 4px 24px #0008',
  color: '#e0e6f0',
};

export default function DataDialog({ open, onClose, datasetInfo, data, loading, error }: DataDialogProps) {
  if (!open) return null;

  return (
    <div style={dialogStyle} onClick={onClose}>
      <div style={modalStyle} onClick={e => e.stopPropagation()}>
        <button style={{ float: 'right', background: 'none', border: 'none', color: '#7ab7ff', fontSize: 22, cursor: 'pointer' }} onClick={onClose}>&times;</button>
        <h2 style={{ marginTop: 0, color: '#7ab7ff' }}>{datasetInfo ? `데이터 미리보기: ${datasetInfo}` : '데이터 미리보기'}</h2>
        {loading && <div style={{ color: '#bbb', margin: '2rem 0' }}>로딩 중...</div>}
        {error && <div style={{ color: '#ff5555', margin: '2rem 0' }}>{error}</div>}
        {!loading && !error && data && (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ borderCollapse: 'collapse', width: '100%', background: '#181b20' }}>
              <thead>
                <tr>
                  {Object.keys(data).map(col => (
                    <th key={col} style={{ border: '1px solid #2c313a', padding: '0.5rem', color: '#7ab7ff', background: '#23272f', position: 'sticky', top: 0 }}>{col}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {(() => {
                  const columns = Object.keys(data);
                  // 각 컬럼의 값을 배열로 변환
                  const colArrays = columns.map(col => Array.isArray(data[col]) ? data[col] : Object.values(data[col] || {}));
                  // 미리보기: 최대 15개 행만 출력
                  const maxRows = Math.min(15, Math.max(...colArrays.map(arr => arr.length), 0));
                  if (!columns.length || maxRows === 0) {
                    return (
                      <tr><td colSpan={columns.length} style={{ color: '#bbb', textAlign: 'center', padding: '2rem' }}>데이터 없음</td></tr>
                    );
                  }
                  return Array.from({ length: maxRows }).map((_, rowIdx) => (
                    <tr key={rowIdx}>
                      {colArrays.map((arr, colIdx) => (
                        <td key={columns[colIdx]} style={{ border: '1px solid #2c313a', padding: '0.5rem', color: '#e0e6f0' }}>{arr[rowIdx] !== undefined ? arr[rowIdx] : ''}</td>
                      ))}
                    </tr>
                  ));
                })()}
              </tbody>
            </table>
            {(!Object.keys(data).length || Object.keys(data).every(col => {
              const arr = Array.isArray(data[col]) ? data[col] : Object.values(data[col] || {});
              return arr.length === 0;
            })) && <div style={{ color: '#bbb', margin: '2rem 0' }}>데이터 없음</div>}
          </div>
        )}
      </div>
    </div>
  );
}
