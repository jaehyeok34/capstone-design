import { useState, useEffect } from 'react';
import ModalNavigation from './ModalNavigation';
import folderIcon from '../assets/illustration/folder.png';
import keyIcon from '../assets/illustration/key.png';

const JoinModal = ({isOpen, onClose}) => {
  const [currentStep, setCurrentStep] = useState(1);
  const [uploadedFiles, setUploadedFiles] = useState([]);
  const [candidateColumns, setCandidateColumns] = useState({});
  const [isFinding, setIsFinding] = useState(false);
  const [isFindCompleted, setIsFindCompleted] = useState(false);
  const [projectName, setProjectName] = useState("");
  const [processingType, setProcessingType] = useState("join");
  const [previewFile, setPreviewFile] = useState(null);
  const [previewContent, setPreviewContent] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    initialize();
  }, [isOpen]);

  // 파일 업로드 핸들러
  const uploadFileHandler = (event) => {
    const files = Array.from(event.target.files);
    const allowedExtensions = ['.csv', '.xlsx', '.xls', '.json', '.tsv'];
    
    // 파일 형식 검증
    const [valid, invalid] = [[], []];
    for (const file of files) {
      const fileName = file.name.toLowerCase();
      if (allowedExtensions.some(ext => fileName.endsWith(ext))) {
        valid.push(file);
      } else {
        invalid.push(file);
      }
    }

    // 유효하지 않은 파일 알림
    if (invalid.length > 0) {
      alert(`다음 파일들은 지원하지 않는 형식입니다:\n${invalid.map(f => f.name).join('\n')}\n\n지원 형식: CSV, Excel, JSON, TSV`);
    }

    // 유효한 파일 추가
    setUploadedFiles(prev => [...prev, ...valid]);
  };

  // 파일 삭제
  const removeFileHandler = (index) => {
    setUploadedFiles(prev => prev.filter((_, idx) => idx !== index));
  };

  // 파일 미리보기
  const filePreviewHandler = (file) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      setPreviewContent(e.target.result);
      setPreviewFile(file);
    };

    if (file.type.startsWith('text/') || file.name.endsWith('.md') || file.name.endsWith('.txt')) {
      reader.readAsText(file);
    } else if (file.type.startsWith('image/')) {
      reader.readAsDataURL(file);
    } else {
      setPreviewContent('이 파일 형식은 미리보기를 지원하지 않습니다.');
      setPreviewFile(file);
    }
  };

  const closePreview = () => {
    setPreviewFile(null);
    setPreviewContent('');
  };

  // 결합키 생성 후보 컬럼 탐색
  const findCandidateColumnsHandler = async () => {
    setIsFinding(true);

    const formData = new FormData();
    uploadedFiles.forEach(file => formData.append("files", file));

    const response = await fetch("http://localhost:8000/api/find_candidate_columns", {
      method: "POST",
      body: formData
    })

    if (!response.ok) {
      alert(`후보 컬럼 탐색 실패: ${response.status}`);
    }

    const candidateColumns = await response.json();

    // 상태 업데이트
    setCandidateColumns(candidateColumns);
    setIsFinding(false);
    setIsFindCompleted(true);
  };

  const closeHandler = () => {
    initialize();
    onClose();
  };

  // 모달 초기화
  const initialize = () => {
    console.log("모달 초기화");
    setCurrentStep(1);
    setUploadedFiles([]);
    setCandidateColumns({});
    setIsFinding(false);
    setIsFindCompleted(false);
    setProjectName("");
    setProcessingType("join");
    setPreviewFile(null);
    setPreviewContent("");
    setIsProcessing(false);
  };

  // 결합 요청 처리
  const submitHandler = async () => {
    setIsProcessing(true);
    
    const formData = new FormData();
    formData.append('projectName', projectName);
    // formData.append('processingType', processingType);
  
    if (Object.keys(candidateColumns).length > 0) {
      formData.append("candidateColumns", JSON.stringify(candidateColumns))
    }
    
    uploadedFiles.forEach((file) => formData.append('files', file));

    const response = await fetch("http://localhost:8000/api/create_project", {
      method: "POST", 
      body: formData
    });

    if (!response.ok) {
      alert(`프로젝트 생성 실패: ${response.status}`);
      return;
    }

    const projectId = await response.text();
    alert(`프로젝트 "${projectName}"가 성공적으로 생성되었습니다! (ID: ${projectId})`);
    closeHandler();
  };

  if (!isOpen) {
    return null;
  }

  return (
    <>
      {/* 메인 모달 */}
      <div 
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          zIndex: 9999,
          background: 'linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1))',
          backdropFilter: 'blur(10px)',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '20px',
          gap: '0'
        }}
        onClick={(e) => {
          if (e.target === e.currentTarget) closeHandler();
        }}
      >
        <div 
          style={{
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            borderRadius: '24px 24px 0 0',
            maxWidth: '800px',
            width: '100%',
            maxHeight: '85vh',
            overflowY: 'auto',
            position: 'relative',
            zIndex: 10000,
            color: '#1f2937',
            boxShadow: '0 25px 50px rgba(0, 0, 0, 0.15)',
            border: '1px solid rgba(255, 255, 255, 0.3)',
            borderBottom: 'none',
            animation: 'slideIn 0.3s ease-out'
          }}
        >
          {/* X 버튼 - 오른쪽 상단 고정 */}
          <button 
            onClick={closeHandler} 
            style={{
              position: 'absolute',
              top: '20px',
              right: '20px',
              width: '40px',
              height: '40px',
              backgroundColor: 'rgba(148, 163, 184, 0.1)',
              border: '1px solid rgba(148, 163, 184, 0.2)',
              borderRadius: '50%',
              color: '#64748b',
              fontSize: '18px',
              cursor: 'pointer',
              transition: 'all 0.3s ease',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              zIndex: 10
            }}
            onMouseEnter={(e) => {
              e.target.style.backgroundColor = 'rgba(239, 68, 68, 0.1)';
              e.target.style.borderColor = 'rgba(239, 68, 68, 0.3)';
              e.target.style.color = '#ef4444';
              e.target.style.transform = 'scale(1.1)';
            }}
            onMouseLeave={(e) => {
              e.target.style.backgroundColor = 'rgba(148, 163, 184, 0.1)';
              e.target.style.borderColor = 'rgba(148, 163, 184, 0.2)';
              e.target.style.color = '#64748b';
              e.target.style.transform = 'scale(1)';
            }}
          >×</button>

          {/* 1단계: 파일 업로드 */}
          {currentStep === 1 && (
            <div style={{padding: '40px', paddingTop: '80px', paddingBottom: '120px'}}>
              {/* 헤더 섹션 */}
              <div style={{textAlign: 'center', marginBottom: '40px'}}>
                <div style={{fontSize: '3rem', marginBottom: '20px'}}>
                  <img src={folderIcon} alt="folder" style={{ width: 50, height: 50, verticalAlign: 'middle', marginBottom: 12 }} />
                </div>
                <h2 style={{fontSize: '2rem', fontWeight: '700', color: '#667eea', margin: '0 0 12px 0'}}>
                  파일 업로드
                </h2>
                <p style={{color: '#64748b', fontSize: '1.1rem', margin: '0'}}>
                  결합할 파일들을 선택해주세요
                </p>
              </div>
              <div 
                style={{
                  border: '3px dashed rgba(102, 126, 234, 0.3)',
                  borderRadius: '20px',
                  padding: '40px',
                  textAlign: 'center',
                  backgroundColor: 'rgba(102, 126, 234, 0.02)',
                  transition: 'all 0.3s ease',
                  cursor: 'pointer'
                }}
                onMouseEnter={(e) => {
                  e.target.style.borderColor = 'rgba(102, 126, 234, 0.5)';
                  e.target.style.backgroundColor = 'rgba(102, 126, 234, 0.05)';
                }}
                onMouseLeave={(e) => {
                  e.target.style.borderColor = 'rgba(102, 126, 234, 0.3)';
                  e.target.style.backgroundColor = 'rgba(102, 126, 234, 0.02)';
                }}
              >
                <div 
                  style={{ 
                    fontSize: '4rem',
                    marginBottom: '20px'
                  }}
                ></div>
                
                <label 
                  style={{
                  display: 'inline-block',
                  padding: '14px 28px',
                  background: 'linear-gradient(135deg, #667eea, #764ba2)',
                  color: 'white',
                  borderRadius: '12px',
                  fontSize: '1rem',
                  fontWeight: '600',
                  cursor: 'pointer',
                  transition: 'all 0.3s ease',
                  boxShadow: '0 4px 15px rgba(102, 126, 234, 0.3)',
                  border: 'none'
                  }}
                  onMouseEnter={(e) => {
                    e.target.style.background = 'linear-gradient(135deg, #5a67d8, #6b46c1)';
                    e.target.style.transform = 'translateY(-2px)';
                    e.target.style.boxShadow = '0 6px 20px rgba(102, 126, 234, 0.4)';
                  }}
                  onMouseLeave={(e) => {
                    e.target.style.background = 'linear-gradient(135deg, #667eea, #764ba2)';
                    e.target.style.transform = 'translateY(0)';
                    e.target.style.boxShadow = '0 4px 15px rgba(102, 126, 234, 0.3)';
                  }}
                >
                  <input type="file" multiple accept=".csv,.xlsx,.xls,.json,.tsv" onChange={uploadFileHandler} style={{ display: 'none' }}/>
                   파일 추가하기
                </label>
                
                <p style={{marginTop: '20px', fontSize: '0.9rem', color: '#64748b', textAlign: 'center'}}>
                  지원형식: CSV, Excel, JSON, Markdown, TSV
                </p>
              </div>

              {uploadedFiles.length > 0 && (
                <div style={{
                  marginTop: '30px',
                  padding: '20px',
                  backgroundColor: 'rgba(16, 185, 129, 0.05)',
                  borderRadius: '16px',
                  border: '1px solid rgba(16, 185, 129, 0.2)'
                }}>
                  <p style={{
                    color: '#10b981',
                    fontWeight: '600',
                    margin: '0 0 8px 0',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px'
                  }}>
                    <span>✅</span>
                    {uploadedFiles.length}개 파일이 추가되었습니다.
                  </p>
                  <p style={{
                    fontSize: '0.9rem',
                    color: '#059669',
                    margin: '0'
                  }}>
                    {uploadedFiles.map(f => f.name).slice(0, 3).join(', ')}
                    {uploadedFiles.length > 3 && ` 외 ${uploadedFiles.length - 3}개`}
                  </p>
                </div>
              )}
            </div>
          )}

          {/* 2단계: 파일 관리 */}
          {currentStep === 2 && (
            <div style={{padding: '40px', paddingTop: '80px', paddingBottom: '120px'}}>
              {/* 헤더 섹션 */}
              <div style={{textAlign: 'center', marginBottom: '40px'}}>
                <div style={{fontSize: '3rem', marginBottom: '20px'}}/>
                <h2 style={{fontSize: '2rem', fontWeight: '700', color: '#667eea', margin: '0 0 12px 0'}}>
                  파일 관리
                </h2>
                <p style={{color: '#64748b', fontSize: '1.1rem', margin: '0'}}>
                  선택된 파일들을 확인하고 관리하세요
                </p>
              </div>
              <div className="space-y-4 max-h-80 overflow-y-auto">
                {uploadedFiles.map((file, index) => (
                  <div 
                    key={index}
                    style={{
                      backgroundColor: 'rgba(102, 126, 234, 0.05)',
                      border: '1px solid rgba(102, 126, 234, 0.2)',
                      borderRadius: '16px',
                      padding: '20px',
                      marginBottom: '12px',
                      transition: 'all 0.3s ease',
                      boxShadow: '0 2px 8px rgba(102, 126, 234, 0.1)'
                    }}
                    onMouseEnter={(e) => {
                      e.target.style.backgroundColor = 'rgba(102, 126, 234, 0.08)';
                      e.target.style.borderColor = 'rgba(102, 126, 234, 0.3)';
                      e.target.style.transform = 'translateY(-2px)';
                      e.target.style.boxShadow = '0 4px 12px rgba(102, 126, 234, 0.15)';
                    }}
                    onMouseLeave={(e) => {
                      e.target.style.backgroundColor = 'rgba(102, 126, 234, 0.05)';
                      e.target.style.borderColor = 'rgba(102, 126, 234, 0.2)';
                      e.target.style.transform = 'translateY(0)';
                      e.target.style.boxShadow = '0 2px 8px rgba(102, 126, 234, 0.1)';
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        {/* 파일 아이콘 */}
                        <div 
                          style={{
                            width: '48px',
                            height: '48px',
                            background: 'linear-gradient(135deg, #667eea, #764ba2)',
                            borderRadius: '12px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center'
                          }}
                        >
                          <span style={{ color: 'white', fontWeight: 'bold', fontSize: '12px' }}>
                            {file.name.split('.').pop()?.toUpperCase() || 'FILE'}
                          </span>
                        </div>
                        <div>
                          <div style={{ fontWeight: '600', color: '#1f2937', fontSize: '16px' }}>
                            {file.name}
                          </div>
                          <div style={{ fontSize: '14px', color: '#667eea', fontWeight: '500' }}>
                            크기: {(file.size / 1024).toFixed(1)} KB
                          </div>
                        </div>
                      </div>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          onClick={() => filePreviewHandler(file)}
                          style={{
                            padding: '8px 16px',
                            background: 'linear-gradient(135deg, #667eea, #764ba2)',
                            color: 'white',
                            border: 'none',
                            borderRadius: '10px',
                            fontSize: '14px',
                            fontWeight: '500',
                            cursor: 'pointer',
                            transition: 'all 0.3s ease',
                            boxShadow: '0 4px 12px rgba(102, 126, 234, 0.3)',
                            transform: 'translateY(0)',
                          }}
                          onMouseEnter={(e) => {
                            e.target.style.transform = 'translateY(-2px)';
                            e.target.style.boxShadow = '0 6px 20px rgba(102, 126, 234, 0.4)';
                            e.target.style.background = 'linear-gradient(135deg, #5a6de8, #6b4fb2)';
                          }}
                          onMouseLeave={(e) => {
                            e.target.style.transform = 'translateY(0)';
                            e.target.style.boxShadow = '0 4px 12px rgba(102, 126, 234, 0.3)';
                            e.target.style.background = 'linear-gradient(135deg, #667eea, #764ba2)';
                          }}
                        >미리보기</button>
                        <button
                          onClick={() => removeFileHandler(index)}
                          style={{
                            padding: '8px 16px',
                            background: 'linear-gradient(135deg, #ef4444, #dc2626)',
                            color: 'white',
                            border: 'none',
                            borderRadius: '10px',
                            fontSize: '14px',
                            fontWeight: '500',
                            cursor: 'pointer',
                            transition: 'all 0.3s ease',
                            boxShadow: '0 4px 12px rgba(239, 68, 68, 0.3)',
                            transform: 'translateY(0)',
                          }}
                          onMouseEnter={(e) => {
                            e.target.style.transform = 'translateY(-2px)';
                            e.target.style.boxShadow = '0 6px 20px rgba(239, 68, 68, 0.4)';
                            e.target.style.background = 'linear-gradient(135deg, #dc2626, #b91c1c)';
                          }}
                          onMouseLeave={(e) => {
                            e.target.style.transform = 'translateY(0)';
                            e.target.style.boxShadow = '0 4px 12px rgba(239, 68, 68, 0.3)';
                            e.target.style.background = 'linear-gradient(135deg, #ef4444, #dc2626)';
                          }}
                        >삭제</button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {/* 결합키 생성 후보 컬럼 분석 섹션 */}
              {uploadedFiles.length >= 2 && (
                <div style={{
                  marginTop: '40px',
                  padding: '30px',
                  backgroundColor: 'rgba(16, 185, 129, 0.05)',
                  borderRadius: '16px',
                  border: '1px solid rgba(16, 185, 129, 0.2)'
                }}>
                  <h3 style={{
                    color: '#334155',
                    fontSize: '1.4rem',
                    marginBottom: '20px',
                    fontWeight: '600',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '10px'
                  }}>
                    <span><img src={keyIcon} alt="key" style={{ width: 20, height: 20, verticalAlign: 'middle' }}/></span>
                    결합키 생성 후보 컬럼 자동 탐색
                  </h3>
                  <div style={{display: 'flex', alignItems: 'center', gap: '20px', flexWrap: 'wrap'}}>
                    <button
                      onClick={findCandidateColumnsHandler}
                      disabled={isFinding}
                      style={{
                        padding: '12px 24px',
                        backgroundColor: isFinding ? '#94a3b8' : '#10b981',
                        color: 'white',
                        border: 'none',
                        borderRadius: '12px',
                        fontSize: '1rem',
                        fontWeight: '600',
                        cursor: isFinding ? 'not-allowed' : 'pointer',
                        transition: 'all 0.3s ease',
                        boxShadow: '0 4px 12px rgba(16, 185, 129, 0.3)',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px'
                      }}
                    >{isFinding ? (<>탐색 중...</>) : (<>후보 탐색</>)}</button>
                    {isFindCompleted && (
                      <div style={{color: '#059669', fontSize: '0.9rem', fontWeight: '500'}}>
                        ✅ 분석 완료: {Object.keys(candidateColumns).length}개 후보 발견
                      </div>
                    )}
                  </div>
                  {/* 결합키 결과 표시 */}
                  {isFindCompleted &&  (
                    <div style={{ marginTop: '24px' }}>
                      <h4 style={{color: '#475569', fontSize: '1.1rem', marginBottom: '16px', fontWeight: '600'}}>
                        발견된 결합키 후보:
                      </h4>
                      <div style={{maxHeight: '200px', overflowY: 'auto', padding: "8px"}}>
                        {Object.entries(candidateColumns).map(([fileName, columns]) => (
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
                            textOverflow: "ellipsis"
                          }}>
                            {fileName}: [<span style={{color: "#059669", fontWeight: "700"}}>{columns.join(", ")}</span>]
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* 3단계: 프로젝트명 입력 */}
          {currentStep === 3 && (
            <div style={{
              padding: '40px 32px 80px 32px',
              background: 'rgba(255, 255, 255, 0.05)',
              backdropFilter: 'blur(10px)',
              borderRadius: '20px',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              margin: '20px'
            }}>
              <div style={{ textAlign: 'center', marginBottom: '32px' }}>
                <h2 style={{
                  fontSize: '32px',
                  fontWeight: 'bold',
                  background: 'linear-gradient(135deg, #667eea, #764ba2)',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  marginBottom: '16px'
                }}> 프로젝트 설정</h2>
                <p style={{
                  color: '#64748b',
                  fontSize: '18px',
                  fontWeight: '500'
                }}>프로젝트 정보를 입력해주세요</p>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
                <div>
                  <label style={{
                    display: 'block',
                    fontSize: '14px',
                    fontWeight: '600',
                    marginBottom: '8px',
                    color: '#374151'
                  }}>
                    프로젝트명 (폴더명) <span style={{ color: '#ef4444' }}>*</span>
                  </label>
                  <input
                    type="text"
                    value={projectName}
                    onChange={(e) => setProjectName(e.target.value)}
                    placeholder="예: my-project, document-join"
                    style={{
                      width: '100%',
                      padding: '12px 16px',
                      background: 'rgba(255, 255, 255, 0.8)',
                      border: '1px solid rgba(148, 163, 184, 0.3)',
                      borderRadius: '12px',
                      fontSize: '16px',
                      color: '#1f2937',
                      outline: 'none',
                      transition: 'all 0.3s ease',
                      backdropFilter: 'blur(5px)'
                    }}
                    onFocus={(e) => {
                      e.target.style.borderColor = '#667eea';
                      e.target.style.boxShadow = '0 0 0 3px rgba(102, 126, 234, 0.1)';
                    }}
                    onBlur={(e) => {
                      e.target.style.borderColor = 'rgba(148, 163, 184, 0.3)';
                      e.target.style.boxShadow = 'none';
                    }}
                  />
                </div>
                <div>
                  <label style={{
                    display: 'block',
                    fontSize: '14px',
                    fontWeight: '600',
                    marginBottom: '8px',
                    color: '#374151'
                  }}>
                    처리 작업 선택
                  </label>
                  <select
                    value={processingType}
                    onChange={(e) => setProcessingType(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '12px 16px',
                      background: 'rgba(255, 255, 255, 0.8)',
                      border: '1px solid rgba(148, 163, 184, 0.3)',
                      borderRadius: '12px',
                      fontSize: '16px',
                      color: '#1f2937',
                      outline: 'none',
                      transition: 'all 0.3s ease',
                      backdropFilter: 'blur(5px)',
                      cursor: 'pointer'
                    }}
                    onFocus={(e) => {
                      e.target.style.borderColor = '#667eea';
                      e.target.style.boxShadow = '0 0 0 3px rgba(102, 126, 234, 0.1)';
                    }}
                    onBlur={(e) => {
                      e.target.style.borderColor = 'rgba(148, 163, 184, 0.3)';
                      e.target.style.boxShadow = 'none';
                    }}
                  >
                    <option value="join">📄 결합 신청</option>
                  </select>
                </div>
              </div>
            </div>
          )}

          {/* 4단계: 결합 요청 */}
          {currentStep === 4 && (
            <div style={{
              position: 'relative',
              padding: '40px 32px 120px 32px',
              background: 'rgba(255, 255, 255, 0.05)',
              backdropFilter: 'blur(10px)',
              borderRadius: '20px',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              margin: '20px'
            }}>
              <div style={{ textAlign: 'center', marginBottom: '32px' }}>
                <h2 style={{
                  fontSize: '32px',
                  fontWeight: 'bold',
                  background: 'linear-gradient(135deg, #10b981, #059669)',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  marginBottom: '16px'
                }}>결합 요청</h2>
                <p style={{
                  color: '#64748b',
                  fontSize: '18px',
                  fontWeight: '500'
                }}>설정을 확인하고 결합 요청을 완료하세요.</p>
              </div>
              <div style={{
                background: 'linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(16, 185, 129, 0.1))',
                backdropFilter: 'blur(10px)',
                borderRadius: '16px',
                padding: '24px',
                marginBottom: '24px',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)'
              }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontWeight: '600', color: '#374151' }}>📁 프로젝트명:</span>
                    <span style={{ 
                      fontWeight: 'bold', 
                      background: 'linear-gradient(135deg, #667eea, #764ba2)',
                      WebkitBackgroundClip: 'text',
                      WebkitTextFillColor: 'transparent'
                    }}>{projectName}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontWeight: '600', color: '#374151' }}>⚙️ 처리 작업:</span>
                    <span style={{ 
                      fontWeight: 'bold',
                      background: 'linear-gradient(135deg, #10b981, #059669)',
                      WebkitBackgroundClip: 'text',
                      WebkitTextFillColor: 'transparent'
                    }}>
                      {processingType === 'join' ? "결합 신청" : "알 수 없음"}
                    </span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontWeight: '600', color: '#374151' }}>📄 선택된 파일:</span>
                    <span style={{ 
                      fontWeight: 'bold',
                      background: 'linear-gradient(135deg, #8b5cf6, #7c3aed)',
                      WebkitBackgroundClip: 'text',
                      WebkitTextFillColor: 'transparent'
                    }}>{uploadedFiles.length}개</span>
                  </div>
                  <div style={{ marginTop: '16px' }}>
                    <p style={{ 
                      fontSize: '14px', 
                      fontWeight: '600', 
                      color: '#374151', 
                      marginBottom: '8px' 
                    }}>📋 파일 목록:</p>
                    <div style={{
                      background: 'rgba(255, 255, 255, 0.6)',
                      borderRadius: '12px',
                      padding: '12px',
                      border: '1px solid rgba(148, 163, 184, 0.3)',
                      maxHeight: '128px',
                      overflowY: 'auto',
                      backdropFilter: 'blur(5px)'
                    }}>
                      {uploadedFiles.map((file, index) => (
                        <div key={index} style={{ 
                          fontSize: '13px', 
                          color: '#4b5563', 
                          padding: '4px 0',
                          borderBottom: index < uploadedFiles.length - 1 ? '1px solid rgba(148, 163, 184, 0.2)' : 'none'
                        }}>
                          📄 {file.name} <span style={{ color: '#9ca3af' }}>({(file.size / 1024).toFixed(1)} KB)</span>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
        
        {/* 네비게이션 버튼 - JoinModal 하단 외부에 위치 */}
        <ModalNavigation
          currentStep={currentStep}
          totalSteps={4}
          onPrev={() => setCurrentStep(prev => prev - 1)}
          onNext={() => setCurrentStep(prev => prev + 1)}
          onSubmit={submitHandler}
          canProceed={
            currentStep === 1 ? uploadedFiles.length > 1 : 
            currentStep === 3 ? projectName.trim() : 
            true
          }
          isProcessing={isProcessing}
          nextButtonText="다음"
          submitButtonText="결합 요청하기"
        />
      </div>

      {/* 파일 미리보기 모달 */}
      {previewFile && (
        <div style={{
          position: 'fixed',
          inset: '0',
          zIndex: '10001',
          background: 'rgba(0, 0, 0, 0.7)',
          backdropFilter: 'blur(8px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}>
          <div style={{
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            borderRadius: '20px',
            padding: '24px',
            maxWidth: '1024px',
            maxHeight: '80vh',
            width: '100%',
            margin: '0 16px',
            overflow: 'hidden',
            border: '1px solid rgba(255, 255, 255, 0.3)',
            boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)'
          }}>
            <div style={{ 
              display: 'flex', 
              justifyContent: 'space-between', 
              alignItems: 'center', 
              marginBottom: '16px' 
            }}>
              <h3 style={{
                fontSize: '20px',
                fontWeight: '600',
                background: 'linear-gradient(135deg, #667eea, #764ba2)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent'
              }}>👁️ 파일 미리보기: {previewFile.name}</h3>
              <button 
                onClick={closePreview} 
                style={{
                  width: '32px',
                  height: '32px',
                  backgroundColor: 'rgba(148, 163, 184, 0.1)',
                  border: '1px solid rgba(148, 163, 184, 0.2)',
                  borderRadius: '50%',
                  color: '#64748b',
                  fontSize: '20px',
                  cursor: 'pointer',
                  transition: 'all 0.3s ease',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
                onMouseEnter={(e) => {
                  e.target.style.backgroundColor = 'rgba(239, 68, 68, 0.1)';
                  e.target.style.borderColor = 'rgba(239, 68, 68, 0.3)';
                  e.target.style.color = '#ef4444';
                }}
                onMouseLeave={(e) => {
                  e.target.style.backgroundColor = 'rgba(148, 163, 184, 0.1)';
                  e.target.style.borderColor = 'rgba(148, 163, 184, 0.2)';
                  e.target.style.color = '#64748b';
                }}
              >
                ×
              </button>
            </div>
            <div style={{
              overflow: 'auto',
              maxHeight: '60vh',
              background: 'rgba(255, 255, 255, 0.8)',
              backdropFilter: 'blur(10px)',
              border: '1px solid rgba(148, 163, 184, 0.2)',
              borderRadius: '12px',
              padding: '16px'
            }}>
              {previewFile.type.startsWith('image/') ? (
                <img 
                  src={previewContent} 
                  alt={previewFile.name}
                  style={{
                    maxWidth: '100%',
                    height: 'auto',
                    borderRadius: '8px',
                    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)'
                  }}
                />
              ) : (
                <pre style={{
                  whiteSpace: 'pre-wrap',
                  fontSize: '14px',
                  color: '#374151',
                  fontFamily: 'Monaco, "Cascadia Code", "Segoe UI Mono", Consolas, "Courier New", monospace',
                  lineHeight: '1.5',
                  margin: '0'
                }}>{previewContent}</pre>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default JoinModal;