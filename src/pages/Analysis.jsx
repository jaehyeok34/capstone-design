import { useState, useEffect } from "react";
import ProjectDetail from "../components/ProjectDetail";

const Analysis = () => {
  const [joinProjects, setJoinProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedProject, setSelectedProject] = useState(null);

  // 실제 결합 프로젝트 데이터 가져오기
  useEffect(() => {
    const fetchJoinProjects = async () => {
      try {
        setLoading(true);
        const response = await fetch('http://localhost:8000/api/join-projects');
        const data = await response.json();
        
        // 백엔드 데이터를 프론트엔드 형식으로 변환
        const formattedProjects = data.projects.map(project => ({
          id: project.id,
          projectName: project.projectName,
          joinKeys: project.joinKeys.map(key => {
            if (typeof key === 'string') {
              return key;
            } else if (key.column && key.matchedColumn) {
              return `${key.column} ↔ ${key.matchedColumn}`;
            } else if (key.dataA_column && key.dataB_column) {
              return `${key.dataA_column} ↔ ${key.dataB_column}`;
            } else {
              return key.column_name || key.column || '결합키';
            }
          }),
          progress: project.progress || 0,
          status: project.status === "분석 완료" ? "completed" : 
                  project.status === "진행 중" ? "processing" : "pending",
          createdAt: new Date(project.createdAt).toLocaleString('ko-KR'),
          fileCount: project.files ? project.files.length : 0,
          resultFile: project.outputFile || null,
          resultSize: project.outputFile ? "N/A" : null,
          processingType: project.processingType || "일반",
          files: project.files || []
        }));
        
        setJoinProjects(formattedProjects);
      } catch (error) {
        console.error('결합 프로젝트 데이터 가져오기 실패:', error);
        // 오류 발생 시 빈 배열로 설정
        setJoinProjects([]);
      } finally {
        setLoading(false);
      }
    };

    fetchJoinProjects();
  }, []);

  const getStatusColor = (status) => {
    switch (status) {
      case 'completed': return '#10b981';
      case 'processing': return '#f59e0b';
      case 'pending': return '#6b7280';
      case 'failed': return '#ef4444';
      default: return '#6b7280';
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case 'completed': return '완료';
      case 'processing': return '진행중';
      case 'pending': return '대기중';
      case 'failed': return '실패';
      default: return '알 수 없음';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'completed': return '✅';
      case 'processing': return '⏳';
      case 'pending': return '⏸️';
      case 'failed': return '❌';
      default: return '❓';
    }
  };

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
          📊 데이터 결합 분석
        </h1>
        <h2 style={{
          fontSize: '1.8rem',
          fontWeight: '400',
          margin: '0 0 10px 0',
          opacity: '0.9'
        }}>
          결합 프로젝트 현황을 확인하세요
        </h2>
        <p style={{
          fontSize: '1.1rem',
          opacity: '0.8',
          margin: '0'
        }}>
          
        </p>
      </div>

      {/* 메인 컨테이너 */}
      <div style={{
        width: '100%',
        maxWidth: '1200px',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: '24px',
        padding: '40px',
        boxShadow: '0 20px 40px rgba(0,0,0,0.1)',
        backdropFilter: 'blur(10px)'
      }}>
        
        {/* 통계 요약 */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '20px',
          marginBottom: '40px'
        }}>
          <div style={{
            backgroundColor: 'rgba(16, 185, 129, 0.1)',
            border: '1px solid rgba(16, 185, 129, 0.3)',
            borderRadius: '16px',
            padding: '20px',
            textAlign: 'center'
          }}>
            <div style={{ fontSize: '2rem', marginBottom: '8px' }}>✅</div>
            <div style={{ fontSize: '1.5rem', fontWeight: '700', color: '#059669' }}>
              {joinProjects.filter(p => p.status === 'completed').length}
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
            <div style={{ fontSize: '2rem', marginBottom: '8px' }}>⏳</div>
            <div style={{ fontSize: '1.5rem', fontWeight: '700', color: '#d97706' }}>
              {joinProjects.filter(p => p.status === 'processing').length}
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
            <div style={{ fontSize: '2rem', marginBottom: '8px' }}>⏸️</div>
            <div style={{ fontSize: '1.5rem', fontWeight: '700', color: '#6b7280' }}>
              {joinProjects.filter(p => p.status === 'pending').length}
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
            <div style={{ fontSize: '2rem', marginBottom: '8px' }}>📁</div>
            <div style={{ fontSize: '1.5rem', fontWeight: '700', color: '#667eea' }}>
              {joinProjects.length}
            </div>
            <div style={{ color: '#6b7280', fontSize: '0.9rem' }}>전체 프로젝트</div>
          </div>
        </div>

        {/* 프로젝트 리스트 헤더 */}
        <div style={{
          marginBottom: '24px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <h3 style={{
            fontSize: '1.5rem',
            fontWeight: '600',
            color: '#1f2937',
            margin: '0'
          }}>
            📋 결합 프로젝트 목록
          </h3>
          <div style={{
            color: '#6b7280',
            fontSize: '0.9rem'
          }}>
            총 {joinProjects.length}개 프로젝트
          </div>
        </div>

        {/* 로딩 상태 */}
        {loading ? (
          <div style={{
            textAlign: 'center',
            padding: '60px',
            color: '#6b7280'
          }}>
            <div style={{
              fontSize: '3rem',
              marginBottom: '20px',
              animation: 'spin 2s linear infinite'
            }}>
              ⏳
            </div>
            <p>프로젝트 목록을 불러오는 중...</p>
          </div>
        ) : (
          /* 프로젝트 리스트 */
          <div style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '16px'
          }}>
            {joinProjects.map((project) => (
              <div key={project.id} style={{
                backgroundColor: '#f8fafc',
                border: '1px solid #e2e8f0',
                borderRadius: '16px',
                padding: '24px',
                transition: 'all 0.3s ease',
                cursor: 'pointer'
              }}
              onClick={() => setSelectedProject(project)}
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
                {/* 프로젝트 헤더 */}
                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'flex-start',
                  marginBottom: '16px'
                }}>
                  <div>
                    <h4 style={{
                      fontSize: '1.2rem',
                      fontWeight: '600',
                      color: '#1f2937',
                      margin: '0 0 8px 0'
                    }}>
                      📁 {project.projectName}
                    </h4>
                    <div style={{
                      fontSize: '0.9rem',
                      color: '#6b7280'
                    }}>
                      생성일: {project.createdAt} | 파일 수: {project.fileCount}개
                    </div>
                  </div>
                  
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    backgroundColor: getStatusColor(project.status) + '20',
                    border: `1px solid ${getStatusColor(project.status)}40`,
                    borderRadius: '20px',
                    padding: '6px 12px',
                    fontSize: '0.85rem',
                    fontWeight: '500',
                    color: getStatusColor(project.status)
                  }}>
                    <span>{getStatusIcon(project.status)}</span>
                    {getStatusText(project.status)}
                  </div>
                </div>

                {/* 결합키 정보 */}
                <div style={{ marginBottom: '16px' }}>
                  <div style={{
                    fontSize: '0.9rem',
                    color: '#374151',
                    marginBottom: '8px',
                    fontWeight: '500'
                  }}>
                    🔗 결합키:
                  </div>
                  <div style={{
                    display: 'flex',
                    flexWrap: 'wrap',
                    gap: '8px'
                  }}>
                    {(project.joinKeys && project.joinKeys.length > 0) ? (
                      project.joinKeys.map((key, index) => (
                        <span key={index} style={{
                          backgroundColor: '#667eea',
                          color: 'white',
                          padding: '4px 12px',
                          borderRadius: '12px',
                          fontSize: '0.8rem',
                          fontWeight: '500'
                        }}>
                          {key}
                        </span>
                      ))
                    ) : (
                      <span style={{
                        backgroundColor: '#e5e7eb',
                        color: '#6b7280',
                        padding: '4px 12px',
                        borderRadius: '12px',
                        fontSize: '0.8rem',
                        fontWeight: '500'
                      }}>none</span>
                    )}
                  </div>
                </div>

                {/* 진행률 바 */}
                <div style={{ marginBottom: '16px' }}>
                  <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    marginBottom: '8px'
                  }}>
                    <span style={{
                      fontSize: '0.9rem',
                      color: '#374151',
                      fontWeight: '500'
                    }}>
                      ⚡ 진행률
                    </span>
                    <span style={{
                      fontSize: '0.9rem',
                      color: '#667eea',
                      fontWeight: '600'
                    }}>
                      none
                    </span>
                  </div>
                  <div style={{
                    width: '100%',
                    height: '8px',
                    backgroundColor: '#e5e7eb',
                    borderRadius: '4px',
                    overflow: 'hidden'
                  }}>
                    <div style={{
                      width: `0%`,
                      height: '100%',
                      background: 'linear-gradient(90deg, #667eea, #764ba2)',
                      transition: 'width 0.3s ease'
                    }} />
                  </div>
                </div>

                {/* 결과물 정보 */}
                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center'
                }}>
                  <div>
                    <div style={{
                      color: '#6b7280',
                      fontSize: '0.9rem'
                    }}>
                      결과물: none
                    </div>
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
          project={selectedProject} 
          onClose={() => setSelectedProject(null)} 
        />
      )}
    </div>
  );
};

export default Analysis;