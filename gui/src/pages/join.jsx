import { useState } from "react";
import JoinModal from "../components/JoinModal";
import joinIcon from '../assets/illustration/join_white.png';
import startIcon from '../assets/illustration/start_white.png';   

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
      <div style={{textAlign: 'center', marginBottom: '60px', color: 'white'}}>
        <h1 style={{
          fontSize: '3.5rem',
          fontWeight: '700',
          margin: '0 0 20px 0',
          textShadow: '2px 2px 4px rgba(0,0,0,0.3)'
        }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
            <img src={joinIcon} alt="join" style={{ width: 250, height: 200, verticalAlign: 'middle', marginBottom: 12 }} />
            <span>데이터 결합 프로젝트 생성</span>
          </div>
        </h1>
        <h2 style={{fontSize: '1.8rem', fontWeight: '400', margin: '0 0 10px 0', opacity: '0.9'}}>
          데이터 결합을 위해 프로젝트를 생성하세요.
        </h2>
      </div>
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
            단계별 가이드를 통해 데이터를 결합할 수 있습니다. <br/>
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
            <span>
              <img src={startIcon} alt="start" style={{ width: 30, height: 30, verticalAlign: 'middle', marginBottom: 1 }} />
            </span>
            결합 프로젝트 생성하기
          </button>
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