
const ModalNavigation = ({ 
  currentStep, 
  totalSteps, 
  onPrev, 
  onNext, 
  onSubmit,
  canProceed = true,
  isProcessing = false,
  nextButtonText = "다음",
  submitButtonText = "완료"
}) => {
  const isFirstStep = currentStep === 1;
  const isLastStep = currentStep === totalSteps;

  return (
    <div 
      style={{ 
        position: 'relative', 
        zIndex: 10001,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '15px',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        backdropFilter: 'blur(20px)',
        borderRadius: '0 0 24px 24px',
        padding: '15px 25px',
        boxShadow: '0 20px 40px rgba(0, 0, 0, 0.15)',
        border: '1px solid rgba(255, 255, 255, 0.3)',
        borderTop: 'none',
        maxWidth: '800px',
        width: '100%'
      }}
    >
      {/* 단계 표시 */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        color: '#64748b',
        fontSize: '0.9rem',
        fontWeight: '500'
      }}>
        <span>{currentStep}</span>
        <span>/</span>
        <span>{totalSteps}</span>
      </div>

      {/* 구분선 */}
      <div style={{
        width: '1px',
        height: '24px',
        backgroundColor: 'rgba(148, 163, 184, 0.3)'
      }} />

      {/* 이전 버튼 - 첫 번째 단계가 아닐 때만 표시 */}
      {!isFirstStep && (
        <button
          onClick={onPrev}
          style={{
            padding: '10px 20px',
            backgroundColor: 'rgba(148, 163, 184, 0.1)',
            color: '#64748b',
            border: '1px solid rgba(148, 163, 184, 0.2)',
            borderRadius: '12px',
            fontSize: '0.95rem',
            fontWeight: '500',
            cursor: 'pointer',
            transition: 'all 0.3s ease',
            display: 'flex',
            alignItems: 'center',
            gap: '6px'
          }}
          onMouseEnter={(e) => {
            e.target.style.backgroundColor = 'rgba(148, 163, 184, 0.15)';
            e.target.style.transform = 'translateY(-1px)';
            e.target.style.boxShadow = '0 4px 12px rgba(148, 163, 184, 0.2)';
          }}
          onMouseLeave={(e) => {
            e.target.style.backgroundColor = 'rgba(148, 163, 184, 0.1)';
            e.target.style.transform = 'translateY(0)';
            e.target.style.boxShadow = 'none';
          }}
        >
          <span>←</span>
          이전
        </button>
      )}

      {/* 다음/완료 버튼 */}
      <button
        onClick={isLastStep ? onSubmit : onNext}
        disabled={!canProceed || isProcessing}
        style={{
          padding: '12px 24px',
          background: !canProceed || isProcessing
            ? 'rgba(148, 163, 184, 0.3)'
            : isLastStep
            ? 'linear-gradient(135deg, #10b981, #059669)'
            : 'linear-gradient(135deg, #667eea, #764ba2)',
          color: !canProceed || isProcessing ? '#94a3b8' : 'white',
          border: 'none',
          borderRadius: '12px',
          fontSize: '0.95rem',
          fontWeight: '600',
          cursor: !canProceed || isProcessing ? 'not-allowed' : 'pointer',
          transition: 'all 0.3s ease',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          boxShadow: !canProceed || isProcessing 
            ? 'none' 
            : isLastStep
            ? '0 4px 15px rgba(16, 185, 129, 0.3)'
            : '0 4px 15px rgba(102, 126, 234, 0.3)',
          minWidth: '120px',
          justifyContent: 'center'
        }}
        onMouseEnter={(e) => {
          if (!canProceed || isProcessing) return;
          e.target.style.transform = 'translateY(-2px)';
          e.target.style.boxShadow = isLastStep
            ? '0 6px 20px rgba(16, 185, 129, 0.4)'
            : '0 6px 20px rgba(102, 126, 234, 0.4)';
          e.target.style.background = isLastStep
            ? 'linear-gradient(135deg, #059669, #047857)'
            : 'linear-gradient(135deg, #5a67d8, #6b46c1)';
        }}
        onMouseLeave={(e) => {
          if (!canProceed || isProcessing) return;
          e.target.style.transform = 'translateY(0)';
          e.target.style.boxShadow = isLastStep
            ? '0 4px 15px rgba(16, 185, 129, 0.3)'
            : '0 4px 15px rgba(102, 126, 234, 0.3)';
          e.target.style.background = isLastStep
            ? 'linear-gradient(135deg, #10b981, #059669)'
            : 'linear-gradient(135deg, #667eea, #764ba2)';
        }}
      >
        {isProcessing ? (
          <>
            <div style={{
              width: '16px',
              height: '16px',
              border: '2px solid rgba(255, 255, 255, 0.3)',
              borderTop: '2px solid white',
              borderRadius: '50%',
              animation: 'spin 1s linear infinite'
            }} />
            처리 중...
          </>
        ) : isLastStep ? (
          <>
            <span>✓</span>
            {submitButtonText}
          </>
        ) : (
          <>
            {nextButtonText}
            <span>→</span>
          </>
        )}
      </button>

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
};

export default ModalNavigation;