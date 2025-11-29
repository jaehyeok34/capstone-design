import { useState, useEffect } from "react";
import downloadIcon from '../assets/illustration/download_white.png';
import keyIcon from '../assets/illustration/key.png';
import { getStatusColor, getStatusIcon, getStatusText, downloadHandler } from "../utils";  

const ProjectDetail = ({ selectedProject, setSelectedProject, setProjects, onClose }) => {
  const [project, setProject] = useState(selectedProject);
  const [previewingFile, setPreviewingFile] = useState(null);
  const [filePreviewContent, setFilePreviewContent] = useState("");
  const [isFetching, setIsFetching] = useState(false);

  if (!project) return null;

  useEffect(() => {
    setProject(selectedProject);
  }, [selectedProject]);


  // 파일 미리보기 함수
  const filePreviewHandler = async (fileName) => {
    setIsFetching(true);
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
      setIsFetching(false);
    }
  };

  const getFileIcon = (fileName) => {
    return {
      "csv": "📊",
      "xlsx": "📗",
      "xls": "📗",
      "json": "📄",
      "txt": "📝"
    }[fileName.split('.').pop().toLowerCase()] || "📄";
  };

  const createCiHandler = async (projectId) => {
    const response = await fetch(`http://localhost:8000/api/create_ci/${projectId}`)
    if (!response.ok) {
      alert(`결합 연계정보 생성 요청 실패: ${response.status}`);
      return;
    }

    updateProject(projectId);
  }

  const joinHandler = async () => {
    const response = await fetch(`http://localhost:8000/api/join/${project.id}`)
    if (!response.ok) {
      alert(`결합 요청 실패: ${response.status}`);
      return;
    }

    updateProject(project.id);
  }

  const updateProject = async (projectId) => {
    const response = await fetch(`http://localhost:8000/api/project/${projectId}`);
    if (!response.ok) {
      alert(`프로젝트 정보 요청 실패: ${response.status}, ${(await response.json())?.detail}`);
      return;
    }

    const information = await response.json();
    setProjects(prev => ({
      ...prev,
      [projectId]: information
    }));
    setSelectedProject(() => ({ id: projectId, ...information }));
  }

  return (
    <div style={{
      position: 'fixed',
      top: 0, left: 0, right: 0, bottom: 0,
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
            <h2 style={{ fontSize: '1.8rem', fontWeight: '700', color: '#1f2937', margin: '0 0 10px 0' }}>
               {project.projectName}
            </h2>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
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
          
          {/* 닫기 버튼 */}
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
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '20px', marginBottom: '30px' }}>
          <div style={{ backgroundColor: '#f8fafc', borderRadius: '12px', padding: '20px', border: '1px solid #e2e8f0' }}>
            <h3 style={{ fontSize: '1rem', fontWeight: '600', color: '#374151', margin: '0 0 8px 0' }}>
              기본 정보
            </h3>
            <div style={{ color: '#6b7280', fontSize: '0.9rem', lineHeight: '1.6' }}>
              <div>파일 수: {project.files.length}개</div>
              <div>결합 연계정보 생성 여부: {project.ci ? "O" : "X"}</div>
            </div>
          </div>

          <div style={{ backgroundColor: '#f8fafc', borderRadius: '12px', padding: '20px', border: '1px solid #e2e8f0' }}>
            <h3 style={{ fontSize: '1rem', fontWeight: '600', color: '#374151', margin: '0 0 8px 0' }}>
              결과물
            </h3>
            <div style={{ color: '#6b7280', fontSize: '0.9rem' }}>
              {project.status === 'done' ? (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    downloadHandler(project.id, project.projectName);
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
                >
                  다운로드
                  <img src={downloadIcon} alt="download" style={{ width: 18, height: 20, verticalAlign: 'middle', marginRight: 6 }} />
                </button>
              ) : (
                '없음'
              )}
            </div>
          </div>
        </div>

        {/* 결합키 정보 */}
        <div style={{ backgroundColor: '#f0f9ff', borderRadius: '16px', padding: '24px', border: `1px solid rgba(34, 197, 94, 0.93)`, marginBottom: '30px' }}>
          <h3 style={{
            fontSize: '1.2rem',
            fontWeight: '600',
            color: '#059669',
            margin: '0 0 16px 0',
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}>
            <img src={keyIcon} alt="key" style={{ width: 20, height: 20, marginRight: 6 }}/>
            결합키 정보
          </h3>
          {Object.entries(project.candidateColumns).map(([fileName, columns]) => (
            <div key={fileName} style={{
              backgroundColor: "rgba(34, 197, 94, 0.1)",
              border: `1px solid rgba(34, 197, 94, 0.3)`,
              borderRadius: '8px',
              padding: '12px',
              marginBottom: '8px',
              fontSize: '1rem',
              fontWeight: '500',
              whiteSpace: "nowrap",
              overflos: "hidden",
              textOverflow: "ellipsis",
              boxShadow: '0 2px 4px rgba(14, 165, 233, 0.2)',
              color: '#033013ff'
            }}>
              {fileName}: [<span style={{color: "#059669", fontWeight: "700"}}>{columns.join(", ")}</span>]
            </div>
          ))}
        </div>

        {/* 파일 목록 */}
        <div style={{ backgroundColor: '#f9fafb', borderRadius: '16px', padding: '24px', border: '1px solid #d1d5db' }}>
          <h3 style={{
            fontSize: '1.2rem',
            fontWeight: '600',
            color: '#374151',
            margin: '0 0 16px 0',
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}>업로드된 파일</h3>
          {project.files && project.files.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
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
                  }}>{getFileIcon(file)}</div>
                  <div style={{ flex: 1, fontSize: '0.9rem', fontWeight: '500', color: '#374151' }}>
                    {file}
                  </div>
                  <button
                    onClick={() => filePreviewHandler(file)}
                    style={{
                      padding: '6px 12px',
                      backgroundColor: '#3b82f6',
                      color: 'white',
                      border: 'none',
                      borderRadius: '6px',
                      fontSize: '0.8rem',
                      fontWeight: '500',
                      cursor: 'pointer',
                      opacity: isFetching && previewingFile === file ? 0.6 : 1,
                      transition: 'all 0.2s ease'
                    }}
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
                  >{isFetching && previewingFile === file ? '로딩...' : '미리보기'}</button>
                </div>
              ))}
            </div>
          ) : (
            <div style={{ color: '#6b7280', fontSize: '0.9rem', textAlign: 'center', padding: '20px' }}>
              파일 정보가 없습니다.
            </div>
          )}
        </div>

        {/* 결합 연계정보 생성 요청, 결합 요청 버튼 */}
        <div style={{ display: "flex", gap: "12px", paddingTop: "24px"}}>
          <button 
            onClick={() => createCiHandler(project.id)}
            disabled={project.ci}
            style={{ 
              padding: "8px 16px", 
              borderRadius: "6px", 
              border: project.ci ? '1px solid rgba(209, 213, 219, 0.3)' : '1px solid rgba(245, 158, 11, 0.3)',
              backgroundColor: project.ci ? 'rgba(209, 213, 219, 0.1)' : 'rgba(245, 158, 11, 0.1)',
              color: project.ci ? '#9ca3af' : '#d97706',
              cursor: project.ci ? 'not-allowed' : 'pointer'
            }}>결합 연계정보 생성 요청</button>
          <button 
            onClick={joinHandler}
            disabled={(!project.ci && project.status !== "active") || (project.status === "done")}
            style={{ 
              padding: "8px 16px", 
              borderRadius: "6px",
              backgroundColor: ((!project.ci && project.status !== "active") || (project.status === "done")) ? 'rgba(209, 213, 219, 0.1)' : 'rgba(16, 185, 129, 0.1)',
              border: ((!project.ci && project.status !== "active") || (project.status === "done")) ? '1px solid rgba(209, 213, 219, 0.3)' : '1px solid rgba(16, 185, 129, 0.3)',
              color: ((!project.ci && project.status !== "active") || (project.status === "done")) ? '#9ca3af' : '#059669',
              cursor: ((!project.ci && project.status !== "active") || (project.status === "done")) ? 'not-allowed' : 'pointer'
          }}>결합 요청</button>
        </div>
      </div>

      {/* 파일 미리보기 모달 */}
      {previewingFile && (
        <div style={{
          position: 'fixed',
          top: 0, left: 0, right: 0, bottom: 0,
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
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <span style={{ fontSize: '1.2rem' }}>{getFileIcon(previewingFile)}</span>
                <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: '600', color: '#111827' }}>
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
                onMouseEnter={(e) => e.target.style.backgroundColor = '#f3f4f6'}
                onMouseLeave={(e) => e.target.style.backgroundColor = 'transparent'}
              >✕</button>
            </div>

            {/* 미리보기 내용 */}
            <div style={{
              flex: 1,
              overflow: 'auto',
              backgroundColor: '#f9fafb',
              borderRadius: '8px',
              padding: '16px'
            }}>
              {isFetching ? (
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  height: '200px',
                  color: '#6b7280'
                }}>로딩 중...</div>
              ) : (
                <pre style={{
                  margin: 0,
                  fontFamily: 'Monaco, Consolas, "Courier New", monospace',
                  fontSize: '0.85rem',
                  lineHeight: '1.5',
                  color: '#374151',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word'
                }}>{filePreviewContent}</pre>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProjectDetail;
