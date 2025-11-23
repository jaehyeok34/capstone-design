import React from "react";

/**
 * 결합키 목록 및 분석 결과를 보여주는 컴포넌트
 * props:
 *   joinKeys: [{ dataA_column, dataB_column, normalized_name, value_similarity_score, recommended }]
 *   title: string (optional)
 */
function JoinKeyList({ joinKeys = [], title = "결합키 분석 결과" }) {
  if (!joinKeys || joinKeys.length === 0) {
    return (
      <div style={{ color: '#6b7280', fontSize: '0.95rem', padding: '12px 0' }}>
        결합키 후보가 없습니다.
      </div>
    );
  }

  return (
    <div style={{ margin: '12px 0' }}>
      {title && <h4 style={{ marginBottom: '8px', fontWeight: 600 }}>{title}</h4>}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        {joinKeys.map((key, idx) => (
          <div key={idx} style={{
            background: key.recommended ? 'linear-gradient(90deg,#a7f3d0,#fef3c7)' : '#f3f4f6',
            border: key.recommended ? '2px solid #10b981' : '1px solid #e5e7eb',
            borderRadius: '8px',
            padding: '10px 16px',
            display: 'flex',
            alignItems: 'center',
            gap: '16px',
            fontSize: '0.98rem',
            fontWeight: key.recommended ? 600 : 400
          }}>
            <span style={{ color: '#2563eb', fontWeight: 500 }}>{key.dataA_column}</span>
            <span style={{ color: '#6b7280' }}>↔</span>
            <span style={{ color: '#f59e0b', fontWeight: 500 }}>{key.dataB_column}</span>
            <span style={{ marginLeft: 'auto', color: key.recommended ? '#10b981' : '#6b7280', fontWeight: 500 }}>
              유사도: {(key.value_similarity_score * 100).toFixed(1)}%
            </span>
            {key.recommended && <span style={{ marginLeft: '8px', color: '#10b981', fontWeight: 700 }}>🔥 추천</span>}
          </div>
        ))}
      </div>
    </div>
  );
}

export default JoinKeyList;
