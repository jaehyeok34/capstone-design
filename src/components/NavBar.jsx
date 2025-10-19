import { Link } from 'react-router-dom';
import { useState } from 'react';

function NavBar() {
    const [activeTab, setActiveTab] = useState(window.location.pathname);

    const handleTabClick = (path) => {
        setActiveTab(path);
    };

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
                    {/* 홈 버튼 */}
                    <Link 
                        to="/" 
                        onClick={() => handleTabClick('/')}
                        style={{
                            position: 'relative',
                            padding: '12px 24px',
                            borderRadius: '12px',
                            fontWeight: '600',
                            textDecoration: 'none',
                            transition: 'all 0.3s ease',
                            transform: 'scale(1)',
                            background: activeTab === '/' 
                                ? 'linear-gradient(135deg, #3b82f6, #2563eb)' 
                                : 'transparent',
                            color: activeTab === '/' ? 'white' : '#374151',
                            boxShadow: activeTab === '/' 
                                ? '0 8px 25px rgba(59, 130, 246, 0.25)' 
                                : 'none'
                        }}
                        onMouseEnter={(e) => {
                            e.target.style.transform = 'scale(1.05)';
                            if (activeTab !== '/') {
                                e.target.style.backgroundColor = '#dbeafe';
                                e.target.style.color = '#2563eb';
                            }
                        }}
                        onMouseLeave={(e) => {
                            e.target.style.transform = 'scale(1)';
                            if (activeTab !== '/') {
                                e.target.style.backgroundColor = 'transparent';
                                e.target.style.color = '#374151';
                            }
                        }}
                    >
                        변환
                        {activeTab === '/' && (
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

                    {/* 분석 버튼 */}
                    <Link 
                        to="/analysis" 
                        onClick={() => handleTabClick('/analysis')}
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
                    </Link>                    {/* 결합 버튼 */}
                    <Link 
                        to="/join" 
                        onClick={() => handleTabClick('/join')}
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
                </div>
            </div>

        </div>
    );
}

export default NavBar;