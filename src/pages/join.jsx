import React, { useState } from "react";
import JoinModal from "../components/JoinModal";

function Join() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  
  return (
    <div style={{
      width: '100vw',
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      padding: '40px 20px',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      position: 'relative',
      left: '50%',
      right: '50%',
      marginLeft: '-50vw',
      marginRight: '-50vw'
    }}>
      {/* 헤더 섹션 */}
      <div style={{
        textAlign: 'center',
        marginBottom: '60px',
        color: 'white'
      }}>
        <h1 style={{
          fontSize: '3.5rem',
          fontWeight: '700',
          margin: '0 0 20px 0',
          textShadow: '2px 2px 4px rgba(0,0,0,0.3)'
        }}>
          🔗 + 📄
        </h1>
        <h2 style={{
          fontSize: '1.8rem',
          fontWeight: '400',
          margin: '0 0 10px 0',
          opacity: '0.9'
        }}>
          데이터를 결합하세요
        </h2>
        <p style={{
          fontSize: '1.1rem',
          opacity: '0.8',
          margin: '0'
        }}>
          데이터를 결합하여 가치있게 분석해요
        </p>
      </div>

      {/* 메인 컨테이너 */}
      <div style={{
        width: '100%',
        maxWidth: '1000px',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: '24px',
        padding: '60px 40px',
        boxShadow: '0 20px 40px rgba(0,0,0,0.1)',
        backdropFilter: 'blur(10px)',
        textAlign: 'center'
      }}>
        
        {/* 기능 설명 카드들 */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '30px',
          marginBottom: '50px'
        }}>
          
        </div>

        {/* 시작하기 버튼 */}
        <div style={{
          padding: '40px 30px',
          backgroundColor: 'rgba(168, 85, 247, 0.05)',
          borderRadius: '20px',
          border: '1px solid rgba(168, 85, 247, 0.1)'
        }}>
          <h3 style={{
            color: '#334155',
            fontSize: '1.5rem',
            margin: '0 0 20px 0',
            fontWeight: '600'
          }}>
            데이터를 가치있게 사용해보세요.
          </h3>
          <p style={{
            color: '#64748b',
            margin: '0 0 30px 0',
            fontSize: '1rem',
            lineHeight: '1.6'
          }}>
            단계별 가이드를 통해 데이터를 결합할 수 있습니다.
            <br/>
            지원 형식: CSV, Excel, JSON 파일 등
          </p>
          
          <button
            onClick={() => setIsModalOpen(true)}
            style={{
              padding: '16px 32px',
              background: 'linear-gradient(135deg, #667eea, #764ba2)',
              color: 'white',
              border: 'none',
              borderRadius: '16px',
              fontSize: '1.1rem',
              fontWeight: '600',
              cursor: 'pointer',
              transition: 'all 0.3s ease',
              boxShadow: '0 8px 20px rgba(102, 126, 234, 0.3)',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '10px'
            }}
            onMouseEnter={(e) => {
              e.target.style.background = 'linear-gradient(135deg, #5a67d8, #6b46c1)';
              e.target.style.transform = 'translateY(-2px)';
              e.target.style.boxShadow = '0 12px 25px rgba(102, 126, 234, 0.4)';
            }}
            onMouseLeave={(e) => {
              e.target.style.background = 'linear-gradient(135deg, #667eea, #764ba2)';
              e.target.style.transform = 'translateY(0)';
              e.target.style.boxShadow = '0 8px 20px rgba(102, 126, 234, 0.3)';
            }}
          >
            <span>🚀</span>
            결합 요청 시작하기
          </button>
        </div>

        {/* 추가 정보 섹션 */}
        <div style={{
          marginTop: '40px',
          padding: '25px',
          backgroundColor: 'rgba(15, 23, 42, 0.03)',
          borderRadius: '12px',
          border: '1px solid rgba(15, 23, 42, 0.1)'
        }}>
          <div style={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            gap: '30px',
            flexWrap: 'wrap'
          }}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: '1.5rem', marginBottom: '5px' }}>⚡</div>
              <span style={{ color: '#64748b', fontSize: '0.9rem' }}>빠른 처리</span>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: '1.5rem', marginBottom: '5px' }}>🔒</div>
              <span style={{ color: '#64748b', fontSize: '0.9rem' }}>안전한 처리</span>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: '1.5rem', marginBottom: '5px' }}>📊</div>
              <span style={{ color: '#64748b', fontSize: '0.9rem' }}>다양한 형식</span>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: '1.5rem', marginBottom: '5px' }}>💾</div>
              <span style={{ color: '#64748b', fontSize: '0.9rem' }}>자동 저장</span>
            </div>
          </div>
        </div>
      </div>

      <JoinModal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)} 
      />
    </div>
  );
}

export default Join;