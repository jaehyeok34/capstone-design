import { useState, useEffect } from "react";
import ProjectDetail from "../components/ProjectDetail";
import checkIcon from '../assets/illustration/check.png';
import sendwatchIcon from '../assets/illustration/sendwatch.png';
import pauseIcon from '../assets/illustration/pause.png';
import downloadIcon from '../assets/illustration/download_white.png';
import folderIcon from '../assets/illustration/folder.png';
import analyzeIcon from '../assets/illustration/analyze_white.png';
import { getStatusColor, getStatusIcon, getStatusText, downloadHandler } from "../utils";

const Analysis = () => {
  const [projects, setProjects] = useState({});
  const [isFetching, setIsFetching] = useState(true);
  const [selectedProject, setSelectedProject] = useState(null);

  // 실제 결합 프로젝트 데이터 가져오기
  useEffect(() => {
    console.log("이거 두번씩 됨");
    (async () => {
      setIsFetching(true);

      const response = await fetch("http://localhost:8000/api/projects");
      if (response.ok) {
        setProjects(await response.json());
      } else {
        alert(`결합 프로젝트 데이터 가져오기 실패: ${response.status}`);
      }

      setIsFetching(false);
    })();
  }, []);

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
          <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
            <img src={analyzeIcon} alt="analyze" style={{ width: 250, height: 200, verticalAlign: 'middle', marginBottom: 12 }} />
            <span>데이터 결합 분석</span>
          </div>
        </h1>
        <h2 style={{fontSize: '1.8rem', fontWeight: '400', margin: '0 0 10px 0', opacity: '0.9'}}>
          결합 프로젝트 현황을 확인하세요.
        </h2>
        <p style={{fontSize: '1.1rem', opacity: '0.8', margin: '0'}}></p>
      </div>
      <div style={{
        width: '100%',
        maxWidth: '1200px',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: '24px',
        padding: '40px',
        boxShadow: '0 20px 40px rgba(0,0,0,0.1)',
        backdropFilter: 'blur(10px)'
      }}>
        <div style={{display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '20px', marginBottom: '40px'}}>
          <div style={{
            backgroundColor: 'rgba(16, 185, 129, 0.1)',
            border: '1px solid rgba(16, 185, 129, 0.3)',
            borderRadius: '16px',
            padding: '20px',
            textAlign: 'center'
          }}>
            <div style={{ fontSize: '2rem', marginBottom: '8px' }}>
              <img src={checkIcon} alt="completed" style={{ width: 32, height: 32 }} />
            </div>
            <div style={{ fontSize: '1.5rem', fontWeight: '700', color: '#059669' }}>
              {Object.entries(projects).filter(([id, information]) => information.status === "done").length}
            </div>
            <div style={{ color: '#6b7280', fontSize: '0.9rem' }}>완료된 프로젝트</div>
          </div>
          
          <div style={{
            backgroundColor: 'rgba(245, 158, 11, 0.1)',
            border: '1px solid rgba(245, 158, 11, 0.3)',
            borderRadius: '16px',
            padding: '20px',
            textAlign: 'center'
          }}>
            <div style={{ fontSize: '2rem', marginBottom: '8px' }}>
              <img src={sendwatchIcon} alt="processing" style={{ width: 20, height: 32 }} />
            </div>
            <div style={{ fontSize: '1.5rem', fontWeight: '700', color: '#d97706' }}>
              {Object.entries(projects).filter(([id, information]) => information.status === "active").length}
            </div>
            <div style={{ color: '#6b7280', fontSize: '0.9rem' }}>진행중인 프로젝트</div>
          </div>
          
          <div style={{
            backgroundColor: 'rgba(107, 114, 128, 0.1)',
            border: '1px solid rgba(107, 114, 128, 0.3)',
            borderRadius: '16px',
            padding: '20px',
            textAlign: 'center'
          }}>
            <div style={{ fontSize: '2rem', marginBottom: '8px' }}>
              <img src={pauseIcon} alt="pending" style={{ width: 32, height: 32 }} />
            </div>
            <div style={{ fontSize: '1.5rem', fontWeight: '700', color: '#6b7280' }}>
              {Object.entries(projects).filter(([id, information]) => information.status === "idle").length}
            </div>
            <div style={{ color: '#6b7280', fontSize: '0.9rem' }}>대기중인 프로젝트</div>
          </div>
          
          <div style={{
            backgroundColor: 'rgba(102, 126, 234, 0.1)',
            border: '1px solid rgba(102, 126, 234, 0.3)',
            borderRadius: '16px',
            padding: '20px',
            textAlign: 'center'
          }}>
            <div style={{ fontSize: '2rem', marginBottom: '8px' }}>
              <img src={folderIcon} alt="folder" style={{ width: 32, height: 32 }} />
            </div>
            <div style={{ fontSize: '1.5rem', fontWeight: '700', color: '#667eea' }}>
              {Object.keys(projects).length}
            </div>
            <div style={{ color: '#6b7280', fontSize: '0.9rem' }}>전체 프로젝트</div>
          </div>
        </div>

        {/* 프로젝트 리스트 헤더 */}
        <div style={{marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
          <h3 style={{fontSize: '1.5rem', fontWeight: '600', color: '#1f2937', margin: '0'}}>
            결합 프로젝트 목록
          </h3>
          <div style={{color: '#6b7280', fontSize: '0.9rem'}}>
            총 {Object.keys(projects).length}개 프로젝트
          </div>
        </div>

        {/* 로딩 상태 */}
        {isFetching ? (
          <div style={{textAlign: 'center', padding: '60px', color: '#6b7280'}}>
            <div style={{fontSize: '3rem', marginBottom: '20px', animation: 'spin 2s linear infinite'}}>
              <img src={sendwatchIcon} alt="loading" style={{ width: 30, height: 30 }} />
            </div>
            <p>프로젝트 목록을 불러오는 중...</p>
          </div>
        ) : (
          /* 프로젝트 리스트 */
          <div style={{display: 'flex', flexDirection: 'column', gap: '16px'}}>
            {Object.entries(projects).map(([id, information]) => (
              <div key={id} style={{
                backgroundColor: '#f8fafc',
                border: '1px solid #e2e8f0',
                borderRadius: '16px',
                padding: '24px',
                transition: 'all 0.3s ease',
                cursor: 'pointer'
              }}
              onClick={() => setSelectedProject({ id, ...information })}
              onMouseEnter={(e) => {
                e.target.style.backgroundColor = '#f1f5f9';
                e.target.style.borderColor = '#667eea';
                e.target.style.transform = 'translateY(-2px)';
                e.target.style.boxShadow = '0 8px 24px rgba(102, 126, 234, 0.15)';
              }}
              onMouseLeave={(e) => {
                e.target.style.backgroundColor = '#f8fafc';
                e.target.style.borderColor = '#e2e8f0';
                e.target.style.transform = 'translateY(0)';
                e.target.style.boxShadow = 'none';
              }}>
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px'}}>
                  <div>
                    <h4 style={{fontSize: '1.2rem', fontWeight: '600', color: '#1f2937', margin: '0 0 8px 0'}}>
                      <img src={folderIcon} alt="folder" style={{ width: 18, height: 18, marginRight: 8, verticalAlign: 'middle' }} /> 
                      {information.projectName}
                    </h4>
                    <div style={{fontSize: '0.9rem', color: '#6b7280'}}>
                      생성일: {information.createdAt} | 파일 수: {information.files.length}개
                    </div>
                  </div>
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    backgroundColor: getStatusColor(information.status) + '20',
                    border: `1px solid ${getStatusColor(information.status)}40`,
                    borderRadius: '20px',
                    padding: '6px 12px',
                    fontSize: '0.85rem',
                    fontWeight: '500',
                    color: getStatusColor(information.status)
                  }}>
                    <span>{getStatusIcon(information.status)}</span>
                    {getStatusText(information.status)}
                  </div>
                </div>

                {/* 결과물 정보 */}
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                  <div style={{color: '#6b7280', fontSize: '0.9rem'}}>
                    {information.status === "done" ? (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          downloadHandler(id, information.projectName);
                        }}
                        style={{
                              marginLeft: '8px',
                              padding: '6px 16px',
                              backgroundColor: '#667eea',
                              color: 'white',
                              border: 'none',
                              borderRadius: '8px',
                              fontSize: '0.85rem',
                              fontWeight: '500',
                              cursor: 'pointer',
                              transition: 'all 0.2s ease'
                            }}
                            onMouseEnter={(e) => {
                              e.target.style.backgroundColor = '#5568d3';
                              e.target.style.transform = 'scale(1.05)';
                            }}
                            onMouseLeave={(e) => {
                              e.target.style.backgroundColor = '#667eea';
                              e.target.style.transform = 'scale(1)';
                            }}
                      >
                        <img src={downloadIcon} alt="download" style={{ width: 18, height: 20, verticalAlign: 'middle', marginRight: 6 }} />
                        다운로드
                      </button>
                    ) : ("결과물: 처리 중...")}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 애니메이션 스타일 */}
      <style>
        {`
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
        `}
      </style>

      {/* 프로젝트 상세 모달 */}
      {selectedProject && (
        <ProjectDetail 
          selectedProject={selectedProject} 
          setSelectedProject={setSelectedProject}
          setProjects={setProjects} 
          onClose={() => setSelectedProject(null)}/>
      )}
    </div>
  );
};

export default Analysis;