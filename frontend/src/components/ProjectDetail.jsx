import { useState, useEffect } from "react";
import downloadIcon from '../assets/illustration/download_white.png';
import checkIcon from '../assets/illustration/check.png';
import sendwatchIcon from '../assets/illustration/sendwatch.png';
import pauseIcon from '../assets/illustration/pause.png';
import keyIcon from '../assets/illustration/key.png';
import folderIcon from '../assets/illustration/folder.png';

const ProjectDetail = ({ project, onClose }) => {
  const [projectData, setProjectData] = useState(project);
  const [previewingFile, setPreviewingFile] = useState(null);
  const [filePreviewContent, setFilePreviewContent] = useState("");
  const [previewLoading, setPreviewLoading] = useState(false);

  if (!project) return null;

  // 파일 미리보기 함수
  const handleFilePreview = async (fileName) => {
    setPreviewLoading(true);
    setPreviewingFile(fileName);
    
    try {
      // 백엔드에서 파일 내용을 가져오는 API 호출
      const response = await fetch(`http://localhost:8000/api/file-preview/${project.id}/${encodeURIComponent(fileName)}`);
      
      if (response.ok) {
        const data = await response.json();
        setFilePreviewContent(data.content || "파일 내용을 불러올 수 없습니다.");
      } else {
        // 임시로 더미 데이터 표시 (실제 구현 전까지)
        const dummyContent = generateDummyContent(fileName);
        setFilePreviewContent(dummyContent);
      }
    } catch (error) {
      console.error('파일 미리보기 오류:', error);
      // 임시로 더미 데이터 표시
      const dummyContent = generateDummyContent(fileName);
      setFilePreviewContent(dummyContent);
    } finally {
      setPreviewLoading(false);
    }
  };

  // 더미 콘텐츠 생성 (실제 API 구현 전 임시용)
  const generateDummyContent = (fileName) => {
    const extension = fileName.split('.').pop().toLowerCase();
    
    switch (extension) {
      case 'csv':
        return `이름,나이,성별,직업
김철수,28,남,개발자
이영희,32,여,디자이너
박민수,25,남,학생
최지영,29,여,마케터
홍길동,35,남,영업`;
        
      case 'xlsx':
      case 'xls':
        return `Excel 파일 미리보기:
        
A1: 제품명    B1: 가격     C1: 수량
A2: 노트북    B2: 1,200    C2: 5
A3: 마우스    B3: 25       C3: 20
A4: 키보드    B4: 75       C4: 15
A5: 모니터    B5: 300      C5: 8`;
        
      case 'json':
        return `{
  "users": [
    {
      "id": 1,
      "name": "김철수",
      "email": "kim@example.com",
      "age": 28
    },
    {
      "id": 2,
      "name": "이영희",
      "email": "lee@example.com",
      "age": 32
    }
  ],
  "total": 2
}`;
        
      default:
        return `파일명: ${fileName}
파일 형식: ${extension.toUpperCase()}

이 파일의 미리보기를 지원하지 않습니다.
지원 형식: CSV, Excel, JSON`;
    }
  };

  const getFileIcon = (fileName) => {
    const extension = fileName.split('.').pop().toLowerCase();
    switch (extension) {
      case 'csv': return '📊';
      case 'xlsx':
      case 'xls': return '📗';
      case 'json': return '📄';
      case 'txt': return '📝';
      default: return '📄';
    }
  };

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
      case 'completed':
        return <img src={checkIcon} alt="completed" style={{ width: 16, height: 16 }} />;
      case 'processing':
        return <img src={sendwatchIcon} alt="processing" style={{ width: 16, height: 16 }} />;
      case 'pending':
        return <img src={pauseIcon} alt="pending" style={{ width: 16, height: 16 }} />;
      case 'failed':
        return '❌';
      default:
        return '❓';
    }
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(0, 0, 0, 0.5)',
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      zIndex: 1000
    }}>
      <div style={{
        backgroundColor: 'white',
        borderRadius: '24px',
        padding: '40px',
        maxWidth: '800px',
        width: '90%',
        maxHeight: '90%',
        overflowY: 'auto',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)'
      }}>
        {/* 헤더 */}
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          marginBottom: '30px',
          borderBottom: '1px solid #e5e7eb',
          paddingBottom: '20px'
        }}>
          <div>
            <h2 style={{
              fontSize: '1.8rem',
              fontWeight: '700',
              color: '#1f2937',
              margin: '0 0 10px 0'
            }}>
               {project.projectName}
            </h2>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '12px'
            }}>
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                backgroundColor: getStatusColor(project.status) + '20',
                border: `1px solid ${getStatusColor(project.status)}40`,
                borderRadius: '20px',
                padding: '6px 12px',
                fontSize: '0.9rem',
                fontWeight: '500',
                color: getStatusColor(project.status)
              }}>
                <span>{getStatusIcon(project.status)}</span>
                {getStatusText(project.status)}
              </div>
              <span style={{ color: '#6b7280', fontSize: '0.9rem' }}>
                생성일: {project.createdAt}
              </span>
            </div>
          </div>
          
          <button
            onClick={onClose}
            style={{
              backgroundColor: '#f3f4f6',
              border: 'none',
              borderRadius: '12px',
              padding: '8px 16px',
              cursor: 'pointer',
              fontSize: '1.1rem',
              fontWeight: '500',
              color: '#6b7280',
              transition: 'all 0.2s ease'
            }}
            onMouseEnter={(e) => {
              e.target.style.backgroundColor = '#e5e7eb';
              e.target.style.color = '#374151';
            }}
            onMouseLeave={(e) => {
              e.target.style.backgroundColor = '#f3f4f6';
              e.target.style.color = '#6b7280';
            }}
          >
            ✕ 닫기
          </button>
        </div>

        {/* 프로젝트 정보 */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
          gap: '20px',
          marginBottom: '30px'
        }}>
          <div style={{
            backgroundColor: '#f8fafc',
            borderRadius: '12px',
            padding: '20px',
            border: '1px solid #e2e8f0'
          }}>
            <h3 style={{
              fontSize: '1rem',
              fontWeight: '600',
              color: '#374151',
              margin: '0 0 8px 0'
            }}>
              기본 정보
            </h3>
            <div style={{ color: '#6b7280', fontSize: '0.9rem', lineHeight: '1.6' }}>
              <div>처리 유형: {project.processingType}</div>
              <div>파일 수: {project.fileCount}개</div>
            </div>
          </div>

          <div style={{
            backgroundColor: '#f8fafc',
            borderRadius: '12px',
            padding: '20px',
            border: '1px solid #e2e8f0'
          }}>
            <h3 style={{
              fontSize: '1rem',
              fontWeight: '600',
              color: '#374151',
              margin: '0 0 8px 0'
            }}>
              결과물
            </h3>
            <div style={{ color: '#6b7280', fontSize: '0.9rem' }}>
              {project.status === 'completed' && project.reviewStatus === 'approved' ? (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    // 결과 파일 다운로드
                    fetch(`http://localhost:8000/api/admin/join-requests/${project.id}/result`)
                      .then(response => response.blob())
                      .then(blob => {
                        const url = window.URL.createObjectURL(blob);
                        const a = document.createElement('a');
                        a.href = url;
                        a.download = `${project.projectName}_result.csv`;
                        document.body.appendChild(a);
                        a.click();
                        window.URL.revokeObjectURL(url);
                        document.body.removeChild(a);
                      })
                      .catch(error => {
                        console.error('결과 파일 다운로드 실패:', error);
                        alert('결과 파일 다운로드에 실패했습니다.');
                      });
                  }}
                  style={{
                    padding: '10px 20px',
                    backgroundColor: '#0ea5e9',
                    color: 'white',
                    border: 'none',
                    borderRadius: '10px',
                    fontSize: '0.9rem',
                    fontWeight: '500',
                    cursor: 'pointer',
                    transition: 'all 0.2s ease',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px'
                  }}
                  onMouseEnter={(e) => {
                    e.target.style.backgroundColor = '#0284c7';
                    e.target.style.transform = 'scale(1.05)';
                  }}
                  onMouseLeave={(e) => {
                    e.target.style.backgroundColor = '#0ea5e9';
                    e.target.style.transform = 'scale(1)';
                  }}
                >다운로드
                <img src={downloadIcon} alt="download" style={{ width: 18, height: 20, verticalAlign: 'middle', marginRight: 6 }} />
                                  
                </button>
              ) : (
                'none'
              )}
            </div>
          </div>
        </div>

        {/* 결합키 정보 */}
        <div style={{
          backgroundColor: '#f0f9ff',
          borderRadius: '16px',
          padding: '24px',
          border: '1px solid #0ea5e9',
          marginBottom: '30px'
        }}>
          <h3 style={{
            fontSize: '1.2rem',
            fontWeight: '600',
            color: '#0c4a6e',
            margin: '0 0 16px 0',
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}>
            <img src={keyIcon} alt="key" style={{ width: 20, height: 20, marginRight: 6 }} />
            결합키 정보
          </h3>
          
          {project.joinKeys && project.joinKeys.length > 0 ? (
            <div>
              <div style={{
                display: 'flex',
                flexWrap: 'wrap',
                gap: '12px',
                marginBottom: '16px'
              }}>
                {project.joinKeys.map((key, index) => {
                  // key가 객체인 경우 (예: {column: 'id', matchedColumn: 'user_id'})
                  const displayText = typeof key === 'object' 
                    ? `${key.column || key.dataA_column || ''} ↔ ${key.matchedColumn || key.dataB_column || ''}`
                    : key;
                  
                  return (
                    <div key={index} style={{
                      backgroundColor: '#0ea5e9',
                      color: 'white',
                      padding: '8px 16px',
                      borderRadius: '20px',
                      fontSize: '0.9rem',
                      fontWeight: '500',
                      boxShadow: '0 2px 4px rgba(14, 165, 233, 0.2)'
                    }}>
                      {displayText}
                    </div>
                  );
                })}
              </div>
              <div style={{
                color: '#0c4a6e',
                fontSize: '0.9rem',
                fontWeight: '500'
              }}>
                총 {project.joinKeys.length}개의 결합키가 발견되었습니다.
              </div>
            </div>
          ) : (
            <div style={{
              color: '#6b7280',
              fontSize: '0.9rem',
              textAlign: 'center',
              padding: '20px'
            }}>
              결합키 정보가 없습니다.
            </div>
          )}
        </div>

        {/* 파일 목록 */}
        <div style={{
          backgroundColor: '#f9fafb',
          borderRadius: '16px',
          padding: '24px',
          border: '1px solid #d1d5db'
        }}>
          <h3 style={{
            fontSize: '1.2rem',
            fontWeight: '600',
            color: '#374151',
            margin: '0 0 16px 0',
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}>
             업로드된 파일
          </h3>
          
          {project.files && project.files.length > 0 ? (
            <div style={{
              display: 'flex',
              flexDirection: 'column',
              gap: '8px'
            }}>
              {project.files.map((file, index) => (
                <div key={index} style={{
                  backgroundColor: 'white',
                  padding: '12px 16px',
                  borderRadius: '8px',
                  border: '1px solid #e5e7eb',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px'
                }}>
                  <div style={{
                    width: '32px',
                    height: '32px',
                    backgroundColor: '#3b82f6',
                    borderRadius: '8px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '1rem'
                  }}>
                    {getFileIcon(file)}
                  </div>
                  <div style={{
                    flex: 1,
                    fontSize: '0.9rem',
                    fontWeight: '500',
                    color: '#374151'
                  }}>
                    {file}
                  </div>
                  <button
                    onClick={() => handleFilePreview(file)}
                    style={{
                      padding: '6px 12px',
                      backgroundColor: '#3b82f6',
                      color: 'white',
                      border: 'none',
                      borderRadius: '6px',
                      fontSize: '0.8rem',
                      fontWeight: '500',
                      cursor: 'pointer',
                      opacity: previewLoading && previewingFile === file ? 0.6 : 1,
                      transition: 'all 0.2s ease'
                    }}
                    disabled={previewLoading && previewingFile === file}
                    onMouseEnter={(e) => {
                      if (!e.target.disabled) {
                        e.target.style.backgroundColor = '#2563eb';
                      }
                    }}
                    onMouseLeave={(e) => {
                      if (!e.target.disabled) {
                        e.target.style.backgroundColor = '#3b82f6';
                      }
                    }}
                  >
                    {previewLoading && previewingFile === file ? '로딩...' : '미리보기'}
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <div style={{
              color: '#6b7280',
              fontSize: '0.9rem',
              textAlign: 'center',
              padding: '20px'
            }}>
              파일 정보가 없습니다.
            </div>
          )}
        </div>
      </div>

      {/* 파일 미리보기 모달 */}
      {previewingFile && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1001
        }}>
          <div style={{
            backgroundColor: 'white',
            borderRadius: '12px',
            padding: '24px',
            maxWidth: '800px',
            width: '90%',
            maxHeight: '80vh',
            overflow: 'hidden',
            display: 'flex',
            flexDirection: 'column'
          }}>
            {/* 미리보기 헤더 */}
            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: '20px',
              paddingBottom: '16px',
              borderBottom: '1px solid #e5e7eb'
            }}>
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '12px'
              }}>
                <span style={{ fontSize: '1.2rem' }}>{getFileIcon(previewingFile)}</span>
                <h3 style={{
                  margin: 0,
                  fontSize: '1.1rem',
                  fontWeight: '600',
                  color: '#111827'
                }}>
                  {previewingFile}
                </h3>
              </div>
              <button
                onClick={() => {
                  setPreviewingFile(null);
                  setFilePreviewContent("");
                }}
                style={{
                  padding: '8px',
                  backgroundColor: 'transparent',
                  border: 'none',
                  borderRadius: '6px',
                  cursor: 'pointer',
                  fontSize: '1.2rem',
                  color: '#6b7280'
                }}
                onMouseEnter={(e) => {
                  e.target.style.backgroundColor = '#f3f4f6';
                }}
                onMouseLeave={(e) => {
                  e.target.style.backgroundColor = 'transparent';
                }}
              >
                ✕
              </button>
            </div>

            {/* 미리보기 내용 */}
            <div style={{
              flex: 1,
              overflow: 'auto',
              backgroundColor: '#f9fafb',
              borderRadius: '8px',
              padding: '16px'
            }}>
              {previewLoading ? (
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  height: '200px',
                  color: '#6b7280'
                }}>
                  로딩 중...
                </div>
              ) : (
                <pre style={{
                  margin: 0,
                  fontFamily: 'Monaco, Consolas, "Courier New", monospace',
                  fontSize: '0.85rem',
                  lineHeight: '1.5',
                  color: '#374151',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word'
                }}>
                  {filePreviewContent}
                </pre>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProjectDetail;
