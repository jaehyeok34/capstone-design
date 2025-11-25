import { Link, useLocation } from 'react-router-dom';
import { useState, useRef, useEffect } from 'react';


function NavBar() {
    const location = useLocation();
    const [activeTab, setActiveTab] = useState(location.pathname);
    const [showAdmin, setShowAdmin] = useState(false);
    const clickCountRef = useRef(0);
    const clickTimerRef = useRef(null);

    const handleSecretClick = () => {
        clickCountRef.current += 1;
        
        // 클릭 카운트 초기화 타이머 (2초 내에 5번 클릭해야 함)
        if (clickTimerRef.current) {
            clearTimeout(clickTimerRef.current);
        }
        
        clickTimerRef.current = setTimeout(() => {
            clickCountRef.current = 0;
        }, 2000);

        // 5번 클릭하면 관리자 버튼 표시
        if (clickCountRef.current >= 5) {
            setShowAdmin(true);
            clickCountRef.current = 0;
            if (clickTimerRef.current) {
                clearTimeout(clickTimerRef.current);
            }
        }
    };

    useEffect(() => {
        setActiveTab(location.pathname);
    }, [location.pathname]);

    return (
        <div 
            style={{
                position: 'relative',
                top: '24px',
                left: '0',
                right: '0',
                zIndex: 50,
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                width: '100%'
            }}
        >
            {/* 숨겨진 클릭 영역 (오른쪽 상단) */}
            <div
                onClick={handleSecretClick}
                style={{
                    position: 'absolute',
                    top: '-20px',
                    right: '20px',
                    width: '50px',
                    height: '50px',
                    cursor: 'default',
                    zIndex: 100
                }}
                title=""
            />

            {/* 메인 네비게이션 컨테이너 */}
            <div 
                style={{
                    backgroundColor: 'rgba(255, 255, 255, 0.9)',
                    backdropFilter: 'blur(12px)',
                    borderRadius: '16px',
                    boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                    padding: '8px'
                }}
            >
                <div style={{ 
                    display: 'flex', 
                    gap: '4px',
                    alignItems: 'center',
                    justifyContent: 'center'
                }}>
                    
                    {/* 결합 버튼 */}
                    <Link 
                        to="/join" 
                        style={{
                            position: 'relative',
                            padding: '12px 24px',
                            borderRadius: '12px',
                            fontWeight: '600',
                            textDecoration: 'none',
                            transition: 'all 0.3s ease',
                            transform: 'scale(1)',
                            background: activeTab === '/join' 
                                ? 'linear-gradient(135deg, #a855f7, #9333ea)' 
                                : 'transparent',
                            color: activeTab === '/join' ? 'white' : '#374151',
                            boxShadow: activeTab === '/join' 
                                ? '0 8px 25px rgba(147, 51, 234, 0.25)' 
                                : 'none'
                        }}
                        onMouseEnter={(e) => {
                            e.target.style.transform = 'scale(1.05)';
                            if (activeTab !== '/join') {
                                e.target.style.backgroundColor = '#faf5ff';
                                e.target.style.color = '#9333ea';
                            }
                        }}
                        onMouseLeave={(e) => {
                            e.target.style.transform = 'scale(1)';
                            if (activeTab !== '/join') {
                                e.target.style.backgroundColor = 'transparent';
                                e.target.style.color = '#374151';
                            }
                        }}
                    >
                        결합
                        {activeTab === '/join' && (
                            <div style={{
                                position: 'absolute',
                                bottom: '-4px',
                                left: '50%',
                                transform: 'translateX(-50%)',
                                width: '8px',
                                height: '8px',
                                backgroundColor: '#a855f7',
                                borderRadius: '50%'
                            }}></div>
                        )}
                    </Link>

                    {/* 분석 버튼 */}
                    <Link 
                        to="/analysis" 
                        style={{
                            position: 'relative',
                            padding: '12px 24px',
                            borderRadius: '12px',
                            fontWeight: '600',
                            textDecoration: 'none',
                            transition: 'all 0.3s ease',
                            transform: 'scale(1)',
                            background: activeTab === '/analysis' 
                                ? 'linear-gradient(135deg, #3b82f6, #2563eb)' 
                                : 'transparent',
                            color: activeTab === '/analysis' ? 'white' : '#374151',
                            boxShadow: activeTab === '/analysis' 
                                ? '0 8px 25px rgba(59, 130, 246, 0.25)' 
                                : 'none'
                        }}
                        onMouseEnter={(e) => {
                            e.target.style.transform = 'scale(1.05)';
                            if (activeTab !== '/analysis') {
                                e.target.style.backgroundColor = '#dbeafe';
                                e.target.style.color = '#2563eb';
                            }
                        }}
                        onMouseLeave={(e) => {
                            e.target.style.transform = 'scale(1)';
                            if (activeTab !== '/analysis') {
                                e.target.style.backgroundColor = 'transparent';
                                e.target.style.color = '#374151';
                            }
                        }}
                    >
                        분석
                        {activeTab === '/analysis' && (
                            <div style={{
                                position: 'absolute',
                                bottom: '-4px',
                                left: '50%',
                                transform: 'translateX(-50%)',
                                width: '8px',
                                height: '8px',
                                backgroundColor: '#3b82f6',
                                borderRadius: '50%'
                            }}></div>
                        )}
                    </Link>

                    {/* 변환 버튼 */}
                    <Link 
                        to="/convert" 
                        style={{
                            position: 'relative',
                            padding: '12px 24px',
                            borderRadius: '12px',
                            fontWeight: '600',
                            textDecoration: 'none',
                            transition: 'all 0.3s ease',
                            transform: 'scale(1)',
                            background: activeTab === '/convert' 
                                ? 'linear-gradient(135deg, #3b82f6, #2563eb)' 
                                : 'transparent',
                            color: activeTab === '/convert' ? 'white' : '#374151',
                            boxShadow: activeTab === '/convert' 
                                ? '0 8px 25px rgba(59, 130, 246, 0.25)' 
                                : 'none'
                        }}
                        onMouseEnter={(e) => {
                            e.target.style.transform = 'scale(1.05)';
                            if (activeTab !== '/convert') {
                                e.target.style.backgroundColor = '#dbeafe';
                                e.target.style.color = '#2563eb';
                            }
                        }}
                        onMouseLeave={(e) => {
                            e.target.style.transform = 'scale(1)';
                            if (activeTab !== '/convert') {
                                e.target.style.backgroundColor = 'transparent';
                                e.target.style.color = '#374151';
                            }
                        }}
                    >
                        변환
                        {activeTab === '/convert' && (
                            <div style={{
                                position: 'absolute',
                                bottom: '-4px',
                                left: '50%',
                                transform: 'translateX(-50%)',
                                width: '8px',
                                height: '8px',
                                backgroundColor: '#3b82f6',
                                borderRadius: '50%'
                            }}></div>
                        )}
                    </Link>


                    {/* 관리자 버튼 (숨김/표시) */}
                    {showAdmin && (
                        <Link 
                            to="/admin" 
                            style={{
                                position: 'relative',
                                padding: '12px 24px',
                                borderRadius: '12px',
                                fontWeight: '600',
                                textDecoration: 'none',
                                transition: 'all 0.3s ease',
                                transform: 'scale(1)',
                                background: activeTab === '/admin' 
                                    ? 'linear-gradient(135deg, #ef4444, #dc2626)' 
                                    : 'transparent',
                                color: activeTab === '/admin' ? 'white' : '#374151',
                                boxShadow: activeTab === '/admin' 
                                    ? '0 8px 25px rgba(239, 68, 68, 0.25)' 
                                    : 'none',
                                animation: 'fadeIn 0.5s ease-in-out'
                            }}
                            onMouseEnter={(e) => {
                                e.target.style.transform = 'scale(1.05)';
                                if (activeTab !== '/admin') {
                                    e.target.style.backgroundColor = '#fee2e2';
                                    e.target.style.color = '#dc2626';
                                }
                            }}
                            onMouseLeave={(e) => {
                                e.target.style.transform = 'scale(1)';
                                if (activeTab !== '/admin') {
                                    e.target.style.backgroundColor = 'transparent';
                                    e.target.style.color = '#374151';
                                }
                            }}
                        >
                             관리자
                            {activeTab === '/admin' && (
                                <div style={{
                                    position: 'absolute',
                                    bottom: '-4px',
                                    left: '50%',
                                    transform: 'translateX(-50%)',
                                    width: '8px',
                                    height: '8px',
                                    backgroundColor: '#ef4444',
                                    borderRadius: '50%'
                                }}></div>
                            )}
                        </Link>
                    )}
                </div>
            </div>

        </div>
    );
}

export default NavBar;