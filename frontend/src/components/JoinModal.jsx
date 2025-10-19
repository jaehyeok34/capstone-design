import { useState } from 'react';
import ModalNavigation from './ModalNavigation';

const JoinModal = ({ isOpen, onClose }) => {
  const [currentStep, setCurrentStep] = useState(1);
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [projectName, setProjectName] = useState('');
  const [processingType, setProcessingType] = useState('join');
  const [previewFile, setPreviewFile] = useState(null);
  const [previewContent, setPreviewContent] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [joinKeys, setJoinKeys] = useState([]);
  const [isAnalyzingKeys, setIsAnalyzingKeys] = useState(false);
  const [keyAnalysisComplete, setKeyAnalysisComplete] = useState(false);

  // 파일 변경 핸들러
  const handleFileChange = (event) => {
    const files = Array.from(event.target.files);
    const allowedExtensions = ['.csv', '.xlsx', '.xls', '.json', '.tsv'];
    
    // 파일 형식 검증
    const validFiles = files.filter(file => {
      const fileName = file.name.toLowerCase();
      return allowedExtensions.some(ext => fileName.endsWith(ext));
    });
    
    const invalidFiles = files.filter(file => {
      const fileName = file.name.toLowerCase();
      return !allowedExtensions.some(ext => fileName.endsWith(ext));
    });
    
    // 잘못된 파일이 있으면 알림
    if (invalidFiles.length > 0) {
      alert(`다음 파일들은 지원하지 않는 형식입니다:\n${invalidFiles.map(f => f.name).join('\n')}\n\n지원 형식: CSV, Excel, JSON, TSV`);
    }
    
    // 유효한 파일만 추가
    if (validFiles.length > 0) {
      setSelectedFiles(prev => [...prev, ...validFiles]);
    }
  };

  // 파일 삭제
  const removeFile = (indexToRemove) => {
    setSelectedFiles(prev => prev.filter((_, index) => index !== indexToRemove));
  };

  // 파일 미리보기
  const previewFileContent = (file) => {
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

  // 결합키 자동 분석
  const analyzeJoinKeys = async () => {
    if (selectedFiles.length !== 2) {
      alert('결합키 분석을 위해서는 정확히 2개의 파일이 필요합니다.');
      return;
    }

    setIsAnalyzingKeys(true);
    try {
      const formData = new FormData();
      selectedFiles.forEach(file => {
        formData.append('files', file);
      });

      const response = await fetch('http://localhost:8000/api/find-join-keys', {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        throw new Error(`분석 실패: ${response.status}`);
      }

      const result = await response.json();
      
      if (result.error) {
        throw new Error(result.error);
      }

      setJoinKeys(result.join_key_candidates || []);
      setKeyAnalysisComplete(true);
      
      if (result.recommended_keys && result.recommended_keys.length > 0) {
        alert(`✅ ${result.recommended_keys.length}개의 추천 결합키를 찾았습니다!`);
      } else if (result.join_key_candidates && result.join_key_candidates.length > 0) {
        alert(`⚠️ ${result.join_key_candidates.length}개의 결합키 후보를 찾았지만 추천 점수가 낮습니다.`);
      } else {
        alert('❌ 공통 결합키를 찾을 수 없습니다. 수동으로 설정해주세요.');
      }

    } catch (error) {
      console.error('결합키 분석 오류:', error);
      alert(`결합키 분석 중 오류가 발생했습니다: ${error.message}`);
    } finally {
      setIsAnalyzingKeys(false);
    }
  };

  // 다음 단계
  const nextStep = () => {
    if (currentStep === 1 && selectedFiles.length === 0) {
      alert('파일을 먼저 선택해주세요.');
      return;
    }
    if (currentStep === 3 && !projectName.trim()) {
      alert('프로젝트명을 입력해주세요.');
      return;
    }
    setCurrentStep(prev => prev + 1);
  };

  // 이전 단계
  const prevStep = () => {
    setCurrentStep(prev => prev - 1);
  };

  // 모달 닫기 및 초기화
  const handleClose = () => {
    setCurrentStep(1);
    setSelectedFiles([]);
    setProjectName('');
    setProcessingType('join');
    setPreviewFile(null);
    setPreviewContent('');
    setIsProcessing(false);
    onClose();
  };

  // 결합 요청 처리
  const handleSubmit = async () => {
    setIsProcessing(true);
    
    const formData = new FormData();
    formData.append('projectName', projectName);
    formData.append('processingType', processingType);
    
    // 추출된 결합키 정보도 함께 전송
    if (joinKeys && joinKeys.length > 0) {
      formData.append('joinKeys', JSON.stringify(joinKeys));
    }
    
    selectedFiles.forEach((file) => {
      formData.append('files', file);
    });

    try {
      const response = await fetch('http://localhost:8000/api/join', {
        method: 'POST',
        body: formData,
      });

      if (response.ok) {
        const result = await response.json();
        alert(`프로젝트 "${projectName}"가 성공적으로 처리되었습니다!`);
        handleClose();
      } else {
        const error = await response.json();
        alert(`오류: ${error.message || '처리 중 문제가 발생했습니다.'}`);
        setIsProcessing(false);
      }
    } catch (error) {
      console.error('Error:', error);
      alert('서버 연결 오류가 발생했습니다. 백엔드 서버가 실행 중인지 확인해주세요.');
      setIsProcessing(false);
    }
  };

  if (!isOpen) return null;

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
          if (e.target === e.currentTarget) handleClose();
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
            onClick={handleClose} 
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
          >
            ×
          </button>

          {/* 1단계: 파일 업로드 */}
          {currentStep === 1 && (
            <div style={{ 
              padding: '40px',
              paddingTop: '80px',
              paddingBottom: '120px'
            }}>
              {/* 헤더 섹션 */}
              <div style={{
                textAlign: 'center',
                marginBottom: '40px'
              }}>
                <div style={{
                  fontSize: '3rem',
                  marginBottom: '20px'
                }}>
                  📁
                </div>
                <h2 style={{
                  fontSize: '2rem',
                  fontWeight: '700',
                  color: '#667eea',
                  margin: '0 0 12px 0'
                }}>
                  파일 업로드
                </h2>
                <p style={{
                  color: '#64748b',
                  fontSize: '1.1rem',
                  margin: '0'
                }}>
                  결합할 파일들을 선택해주세요
                </p>
              </div>
              
              <div style={{
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
              }}>
                <div style={{ 
                  fontSize: '4rem',
                  marginBottom: '20px'
                }}>
                  ⬆️
                </div>
                
                <label style={{
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
                }}>
                  <input 
                    type="file" 
                    multiple 
                    accept=".csv,.xlsx,.xls,.json,.tsv"
                    onChange={handleFileChange}
                    style={{ display: 'none' }}
                  />
                  📁 파일 선택하기
                </label>
                
                <p style={{
                  marginTop: '20px',
                  fontSize: '0.9rem',
                  color: '#64748b',
                  textAlign: 'center'
                }}>
                  📊 지원 형식: CSV, Excel (.xlsx, .xls), JSON, TSV<br/>
                  여러 파일을 동시에 선택할 수 있습니다
                </p>
              </div>

              {selectedFiles.length > 0 && (
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
                    {selectedFiles.length}개 파일이 선택되었습니다
                  </p>
                  <p style={{
                    fontSize: '0.9rem',
                    color: '#059669',
                    margin: '0'
                  }}>
                    {selectedFiles.map(f => f.name).slice(0, 3).join(', ')}
                    {selectedFiles.length > 3 && ` 외 ${selectedFiles.length - 3}개`}
                  </p>
                </div>
              )}


            </div>
          )}

          {/* 2단계: 파일 관리 */}
          {currentStep === 2 && (
            <div style={{ 
              padding: '40px',
              paddingTop: '80px',
              paddingBottom: '120px'
            }}>
              {/* 헤더 섹션 */}
              <div style={{
                textAlign: 'center',
                marginBottom: '40px'
              }}>
                <div style={{
                  fontSize: '3rem',
                  marginBottom: '20px'
                }}>
                  📋
                </div>
                <h2 style={{
                  fontSize: '2rem',
                  fontWeight: '700',
                  color: '#667eea',
                  margin: '0 0 12px 0'
                }}>
                  파일 관리
                </h2>
                <p style={{
                  color: '#64748b',
                  fontSize: '1.1rem',
                  margin: '0'
                }}>
                  선택된 파일들을 확인하고 관리하세요
                </p>
              </div>
              
              <div className="space-y-4 max-h-80 overflow-y-auto">
                {selectedFiles.map((file, index) => (
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
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
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
                          onClick={() => previewFileContent(file)}
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
                        >
                          미리보기
                        </button>
                        <button
                          onClick={() => removeFile(index)}
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
                        >
                        삭제
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {/* 결합키 분석 섹션 */}
              {selectedFiles.length === 2 && (
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
                    <span>🔗</span>
                    결합키 자동 분석
                  </h3>
                  
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '20px',
                    flexWrap: 'wrap'
                  }}>
                    <button
                      onClick={analyzeJoinKeys}
                      disabled={isAnalyzingKeys}
                      style={{
                        padding: '12px 24px',
                        backgroundColor: isAnalyzingKeys ? '#94a3b8' : '#10b981',
                        color: 'white',
                        border: 'none',
                        borderRadius: '12px',
                        fontSize: '1rem',
                        fontWeight: '600',
                        cursor: isAnalyzingKeys ? 'not-allowed' : 'pointer',
                        transition: 'all 0.3s ease',
                        boxShadow: '0 4px 12px rgba(16, 185, 129, 0.3)',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px'
                      }}
                    >
                      {isAnalyzingKeys ? (
                        <>
                          <span>⏳</span>
                          분석 중...
                        </>
                      ) : (
                        <>
                          <span>🔍</span>
                          결합키 찾기
                        </>
                      )}
                    </button>
                    
                    {keyAnalysisComplete && (
                      <div style={{
                        color: '#059669',
                        fontSize: '0.9rem',
                        fontWeight: '500'
                      }}>
                        ✅ 분석 완료: {joinKeys.length}개 후보 발견
                      </div>
                    )}
                  </div>

                  {/* 결합키 결과 표시 */}
                  {keyAnalysisComplete && joinKeys.length > 0 && (
                    <div style={{ marginTop: '24px' }}>
                      <h4 style={{
                        color: '#475569',
                        fontSize: '1.1rem',
                        marginBottom: '16px',
                        fontWeight: '600'
                      }}>
                        발견된 결합키 후보:
                      </h4>
                      <div style={{ maxHeight: '200px', overflowY: 'auto' }}>
                        {joinKeys.map((key, index) => (
                          <div key={index} style={{
                            backgroundColor: key.recommended ? 'rgba(34, 197, 94, 0.1)' : 'rgba(156, 163, 175, 0.1)',
                            border: `1px solid ${key.recommended ? 'rgba(34, 197, 94, 0.3)' : 'rgba(156, 163, 175, 0.3)'}`,
                            borderRadius: '8px',
                            padding: '12px',
                            marginBottom: '8px',
                            fontSize: '0.9rem'
                          }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                              <div>
                                <strong>{key.dataA_column}</strong> ↔ <strong>{key.dataB_column}</strong>
                                {key.recommended && <span style={{ color: '#059669', marginLeft: '8px' }}>✅ 추천</span>}
                              </div>
                              <div style={{ fontSize: '0.8rem', color: '#64748b' }}>
                                유사도: {(key.value_similarity_score * 100).toFixed(1)}%
                              </div>
                            </div>
                            <div style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '4px' }}>
                              고유비율: {(key.dataA_unique_ratio * 100).toFixed(1)}% / {(key.dataB_unique_ratio * 100).toFixed(1)}%
                            </div>
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
              {/* 중앙 정렬된 제목 */}
              <div style={{ textAlign: 'center', marginBottom: '32px' }}>
                <h2 style={{
                  fontSize: '32px',
                  fontWeight: 'bold',
                  background: 'linear-gradient(135deg, #667eea, #764ba2)',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  marginBottom: '16px'
                }}>🔧 프로젝트 설정</h2>
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
                    📁 프로젝트명 (폴더명) <span style={{ color: '#ef4444' }}>*</span>
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
                    ⚙️ 처리 작업 선택
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
                    <option value="md-conversion">📝 마크다운 변환</option>
                    <option value="format-conversion">🔄 형식 변환</option>
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
              {/* 중앙 정렬된 제목 */}
              <div style={{ textAlign: 'center', marginBottom: '32px' }}>
                <h2 style={{
                  fontSize: '32px',
                  fontWeight: 'bold',
                  background: 'linear-gradient(135deg, #10b981, #059669)',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  marginBottom: '16px'
                }}>🚀 결합 요청</h2>
                <p style={{
                  color: '#64748b',
                  fontSize: '18px',
                  fontWeight: '500'
                }}>설정을 확인하고 결합을 요청하세요</p>
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
                      {processingType === 'join' ? '📄 결합 신청' : 
                       processingType === 'md-conversion' ? '📝 마크다운 변환' : '🔄 형식 변환'}
                    </span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontWeight: '600', color: '#374151' }}>📄 선택된 파일:</span>
                    <span style={{ 
                      fontWeight: 'bold',
                      background: 'linear-gradient(135deg, #8b5cf6, #7c3aed)',
                      WebkitBackgroundClip: 'text',
                      WebkitTextFillColor: 'transparent'
                    }}>{selectedFiles.length}개</span>
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
                      {selectedFiles.map((file, index) => (
                        <div key={index} style={{ 
                          fontSize: '13px', 
                          color: '#4b5563', 
                          padding: '4px 0',
                          borderBottom: index < selectedFiles.length - 1 ? '1px solid rgba(148, 163, 184, 0.2)' : 'none'
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
          onPrev={prevStep}
          onNext={nextStep}
          onSubmit={handleSubmit}
          canProceed={
            currentStep === 1 ? selectedFiles.length > 0 :
            currentStep === 3 ? projectName.trim() !== '' :
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