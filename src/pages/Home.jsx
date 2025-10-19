import { useState } from 'react';
import axios from 'axios';
import ReactMarkdown from 'react-markdown';
import MarkdownViewer from '../components/MarkdownViewer';

function Home() {
  const [markdownFiles, setMarkdownFiles] = useState([]); // 여러 파일의 마크다운 결과 저장
  const [loading, setLoading] = useState(false);
  const [selectedFormat, setSelectedFormat] = useState('md');
  const [dragActive, setDragActive] = useState(false);
  const [uploadedFiles, setUploadedFiles] = useState([]); // 업로드된 파일 목록
  const [currentProcessing, setCurrentProcessing] = useState(''); // 현재 처리 중인 파일명

  const handleFileChange = async (e) => {
    const files = Array.from(e.target.files);
    if (files.length === 0) return;
    handleMultipleFileUpload(files);
  };

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const files = Array.from(e.dataTransfer.files);
      handleMultipleFileUpload(files);
    }
  };

  const handleMultipleFileUpload = async (files) => {
    // 파일 크기 체크 (1MB = 1024*1024 bytes)
    const maxSize = 1024 * 1024; // 1MB
    const largeFiles = files.filter(file => file.size > maxSize);
    
    if (largeFiles.length > 0) {
      const fileNames = largeFiles.map(f => f.name).join(', ');
      const proceed = confirm(`다음 파일들이 1MB보다 큽니다: ${fileNames}\n\n처리에 시간이 오래 걸릴 수 있습니다. 계속하시겠습니까?`);
      if (!proceed) return;
    }
    
    setLoading(true);
    setUploadedFiles(files);
    
    try {
      const markdownResults = [];
      
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        setCurrentProcessing(`${file.name} (${i + 1}/${files.length})`);
        
        const formData = new FormData();
        formData.append('file', file);
        
        const res = await axios.post('http://localhost:8000/api/convert', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
          timeout: 300000, // 5분 타임아웃으로 증가
        });
        
        markdownResults.push({
          fileName: file.name,
          originalFile: file,
          markdown: res.data.markdown
        });
      }
      
      setMarkdownFiles(markdownResults);
    } catch (err) {
      if (err.code === 'ECONNABORTED') {
        alert('파일이 너무 커서 변환에 시간이 오래 걸립니다. 잠시 후 다시 시도해주세요.');
      } else if (err.response) {
        alert(`업로드 실패: ${err.response.status} - ${err.response.data?.detail || '서버 오류'}`);
      } else {
        alert('업로드 실패: 네트워크 오류');
      }
    } finally {
      setLoading(false);
      setCurrentProcessing('');
    }
  };

  const handleDownload = async () => {
    if (markdownFiles.length === 0) {
      alert('변환된 파일이 없습니다!');
      return;
    }

    try {
      setLoading(true);
      
      for (const markdownFile of markdownFiles) {
        const formData = new FormData();
        formData.append('markdown', markdownFile.markdown);
        formData.append('format', selectedFormat);

        const res = await axios.post(
          `http://localhost:8000/api/export`,
          formData,
          { responseType: 'blob' }
        );

        const blob = new Blob([res.data], { type: res.headers['content-type'] });
        const url = window.URL.createObjectURL(blob);

        const a = document.createElement('a');
        a.href = url;
        // 파일명에서 확장자 제거하고 새 확장자 추가
        const baseName = markdownFile.fileName.replace(/\.[^/.]+$/, "");
        a.download = `${baseName}_converted.${selectedFormat}`;
        document.body.appendChild(a);
        a.click();
        a.remove();

        window.URL.revokeObjectURL(url);
        
        // 다운로드 간격 조정 (브라우저 제한 방지)
        await new Promise(resolve => setTimeout(resolve, 500));
      }
    } catch (err) {
      alert('다운로드 실패!');
      console.error(err);
    } finally {
      setLoading(false);
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
          📄 → 📘
        </h1>
        <h2 style={{
          fontSize: '1.8rem',
          fontWeight: '400',
          margin: '0 0 10px 0',
          opacity: '0.9'
        }}>
          파일을 원하는 형식으로 변환하세요
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
        maxWidth: '1000px',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: '24px',
        padding: '40px',
        boxShadow: '0 20px 40px rgba(0,0,0,0.1)',
        backdropFilter: 'blur(10px)'
      }}>
        
        {/* 파일 업로드 영역 */}
        <div
          style={{
            border: dragActive 
              ? '3px dashed #667eea' 
              : '3px dashed #e2e8f0',
            borderRadius: '16px',
            padding: '60px 40px',
            textAlign: 'center',
            marginBottom: '40px',
            backgroundColor: dragActive 
              ? 'rgba(102, 126, 234, 0.05)' 
              : 'rgba(248, 250, 252, 0.8)',
            transition: 'all 0.3s ease',
            cursor: 'pointer',
            position: 'relative'
          }}
          onDragEnter={handleDrag}
          onDragLeave={handleDrag}
          onDragOver={handleDrag}
          onDrop={handleDrop}
          onClick={() => document.getElementById('fileInput').click()}
        >
          <input
            id="fileInput"
            type="file"
            multiple
            accept=".csv,.xlsx,.xls,.json,.docx,.pptx,.pdf,.txt"
            onChange={handleFileChange}
            style={{ display: 'none' }}
          />
          
          {loading ? (
            <div>
              <div style={{
                fontSize: '4rem',
                marginBottom: '20px',
                display: 'inline-block',
                animation: 'spin 2s linear infinite'
              }}>
                ⏳
              </div>
              <h3 style={{ 
                color: '#667eea',
                fontSize: '1.5rem',
                margin: '0 0 10px 0'
              }}>
                변환 중입니다...
              </h3>
              <p style={{ 
                color: '#64748b',
                margin: '0 0 10px 0'
              }}>
                {currentProcessing ? `처리 중: ${currentProcessing}` : '잠시만 기다려주세요'}
              </p>
              <p style={{ 
                color: '#94a3b8',
                margin: '0',
                fontSize: '0.9rem'
              }}>
                대용량 파일은 처리에 시간이 걸릴 수 있습니다
              </p>
            </div>
          ) : (
            <div>
              <div style={{
                fontSize: '4rem',
                marginBottom: '20px'
              }}>
                📁
              </div>
              <h3 style={{ 
                color: '#334155',
                fontSize: '1.5rem',
                margin: '0 0 10px 0'
              }}>
                파일을 드래그하거나 클릭하여 업로드
              </h3>
              <p style={{ 
                color: '#64748b',
                margin: '0 0 20px 0'
              }}>
                📄 여러 파일 동시 업로드 지원<br/>
                CSV, Excel, JSON 파일을 지원합니다
              </p>
              <div style={{
                display: 'inline-block',
                padding: '12px 24px',
                backgroundColor: '#667eea',
                color: 'white',
                borderRadius: '12px',
                fontSize: '1rem',
                fontWeight: '600',
                transition: 'all 0.3s ease',
                boxShadow: '0 4px 12px rgba(102, 126, 234, 0.3)'
              }}>
                파일 선택하기
              </div>
            </div>
          )}
        </div>

        {/* 마크다운 미리보기 영역 */}
        {/* 마크다운 결과 표시 */}
        {markdownFiles.length > 0 && (
          <div style={{
            marginTop: '40px',
            padding: '30px',
            backgroundColor: 'rgba(16, 185, 129, 0.05)',
            borderRadius: '16px',
            border: '1px solid rgba(16, 185, 129, 0.1)'
          }}>
            <h3 style={{
              color: '#334155',
              fontSize: '1.4rem',
              marginBottom: '20px',
              fontWeight: '600'
            }}>
              📄 변환된 마크다운 ({markdownFiles.length}개 파일)
            </h3>
            {markdownFiles.map((markdownFile, index) => {
              const fileSizeKB = Math.round(markdownFile.markdown.length / 1024);
              const isLargeFile = markdownFile.markdown.length > 10240; // 10KB
              const previewContent = isLargeFile 
                ? markdownFile.markdown.substring(0, 5120) // 5KB만 미리보기
                : markdownFile.markdown;
              
              return (
                <div key={index} style={{
                  marginBottom: index < markdownFiles.length - 1 ? '30px' : '0'
                }}>
                  <h4 style={{
                    color: '#475569',
                    fontSize: '1.1rem',
                    marginBottom: '10px',
                    fontWeight: '600',
                    backgroundColor: 'rgba(102, 126, 234, 0.1)',
                    padding: '8px 16px',
                    borderRadius: '8px',
                    display: 'inline-block'
                  }}>
                    📄 {markdownFile.fileName} ({fileSizeKB}KB)
                  </h4>
                  {isLargeFile && (
                    <div style={{
                      backgroundColor: '#fef3c7',
                      border: '1px solid #f59e0b',
                      borderRadius: '8px',
                      padding: '12px',
                      marginBottom: '10px',
                      fontSize: '0.9rem',
                      color: '#92400e'
                    }}>
                      ⚠️ 대용량 파일입니다. 일부만 미리보기됩니다. 전체 내용은 다운로드하여 확인하세요.
                    </div>
                  )}
                  <div style={{
                    backgroundColor: '#f8fafc',
                    border: '1px solid #e2e8f0',
                    borderRadius: '12px',
                    overflow: 'hidden',
                    color: '#1f2937',
                    padding: '20px',
                    maxHeight: '400px',
                    overflowY: 'auto'
                  }}>
                    <div style={{
                      color: '#1f2937'
                    }}>
                      <MarkdownViewer content={previewContent} />
                      {isLargeFile && (
                        <div style={{
                          marginTop: '20px',
                          padding: '15px',
                          backgroundColor: '#e0f2fe',
                          borderRadius: '8px',
                          textAlign: 'center',
                          color: '#0369a1',
                          fontWeight: '600'
                        }}>
                          📋 미리보기가 잘렸습니다. 전체 내용을 보려면 다운로드하세요.
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}        {/* 다운로드 섹션 */}
        {markdownFiles.length > 0 && (
          <div style={{
            padding: '30px',
            backgroundColor: 'rgba(102, 126, 234, 0.05)',
            borderRadius: '16px',
            border: '1px solid rgba(102, 126, 234, 0.1)'
          }}>
            <h3 style={{
              color: '#334155',
              fontSize: '1.4rem',
              marginBottom: '20px',
              display: 'flex',
              alignItems: 'center',
              gap: '10px'
            }}>
              <span>💾</span>
              일괄 파일 다운로드 ({markdownFiles.length}개)
            </h3>
            
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '20px',
              flexWrap: 'wrap'
            }}>
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '10px'
              }}>
                <label style={{
                  color: '#475569',
                  fontWeight: '600'
                }}>
                  변환 포맷:
                </label>
                <select
                  value={selectedFormat}
                  onChange={(e) => setSelectedFormat(e.target.value)}
                  style={{
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '2px solid #e2e8f0',
                    backgroundColor: 'white',
                    color: '#334155',
                    fontSize: '1rem',
                    cursor: 'pointer',
                    outline: 'none',
                    transition: 'border-color 0.3s ease'
                  }}
                >
                  <option value="md"> MD (마크다운)</option>
                  <option value="pdf"> PDF</option>
                  <option value="html"> HTML</option>
                  <option value="docx"> DOCX</option>
                  <option value="csv"> CSV</option>
                  <option value="json"> JSON</option>
                </select>
              </div>
              
              <button
                onClick={handleDownload}
                style={{
                  padding: '12px 24px',
                  backgroundColor: '#667eea',
                  color: 'white',
                  border: 'none',
                  borderRadius: '12px',
                  fontSize: '1rem',
                  fontWeight: '600',
                  cursor: 'pointer',
                  transition: 'all 0.3s ease',
                  boxShadow: '0 4px 12px rgba(102, 126, 234, 0.3)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px'
                }}
                onMouseEnter={(e) => {
                  e.target.style.backgroundColor = '#5a67d8';
                  e.target.style.transform = 'translateY(-2px)';
                  e.target.style.boxShadow = '0 6px 16px rgba(102, 126, 234, 0.4)';
                }}
                onMouseLeave={(e) => {
                  e.target.style.backgroundColor = '#667eea';
                  e.target.style.transform = 'translateY(0)';
                  e.target.style.boxShadow = '0 4px 12px rgba(102, 126, 234, 0.3)';
                }}
              >
                <span>⬇️</span>
                다운로드
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 스피너 애니메이션을 위한 스타일 */}
      <style>
        {`
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
        `}
      </style>
    </div>
  );
}

export default Home;