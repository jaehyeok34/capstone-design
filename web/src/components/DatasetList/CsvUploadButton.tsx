import React from 'react';

interface CsvUploadButtonProps {
  onUpload: (file: File) => void;
  loading?: boolean;
}

export default function CsvUploadButton({ onUpload, loading }: CsvUploadButtonProps) {
  const inputRef = React.useRef<HTMLInputElement>(null);

  const handleClick = () => {
    inputRef.current?.click();
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      onUpload(file);
      e.target.value = '';
    }
  };

  return (
    <>
      <button
        type="button"
        style={{
          marginTop: '2rem',
          background: '#23272f',
          color: '#7ab7ff',
          border: '1px solid #7ab7ff',
          borderRadius: 6,
          padding: '0.6rem 1.2rem',
          fontWeight: 600,
          fontSize: '1rem',
          cursor: loading ? 'not-allowed' : 'pointer',
          opacity: loading ? 0.6 : 1,
          width: '100%',
        }}
        onClick={handleClick}
        disabled={loading}
      >
        {loading ? '업로드 중...' : '데이터 등록'}
      </button>
      <input
        ref={inputRef}
        type="file"
        accept=".csv"
        style={{ display: 'none' }}
        onChange={handleChange}
      />
    </>
  );
}
