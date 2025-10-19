import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import "./tableStyle.css"; // Assuming you have styles for tables in this file

const MarkdownViewer = ({ content }: { content: any }) => {
  return (
    <div style={{ 
      color: '#1f2937', 
      fontSize: '1rem',
      lineHeight: '1.6'
    }}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          table: ({node, ...props}) => (
            <table className="custom-table" {...props} />
          ),
          th: ({node, ...props}) => (
            <th className="center-text" {...props} />
          ),
          td: ({node, ...props}) => (
            <td className="center-text" {...props} />
          ),
          p: ({node, ...props}) => (
            <p style={{ color: '#1f2937', margin: '0 0 16px 0' }} {...props} />
          ),
          h1: ({node, ...props}) => (
            <h1 style={{ color: '#111827', fontSize: '1.5rem', fontWeight: '600', margin: '0 0 16px 0' }} {...props} />
          ),
          h2: ({node, ...props}) => (
            <h2 style={{ color: '#111827', fontSize: '1.3rem', fontWeight: '600', margin: '0 0 14px 0' }} {...props} />
          ),
          h3: ({node, ...props}) => (
            <h3 style={{ color: '#111827', fontSize: '1.1rem', fontWeight: '600', margin: '0 0 12px 0' }} {...props} />
          ),
          ul: ({node, ...props}) => (
            <ul style={{ color: '#1f2937', paddingLeft: '20px', margin: '0 0 16px 0' }} {...props} />
          ),
          ol: ({node, ...props}) => (
            <ol style={{ color: '#1f2937', paddingLeft: '20px', margin: '0 0 16px 0' }} {...props} />
          ),
          li: ({node, ...props}) => (
            <li style={{ color: '#1f2937', margin: '0 0 4px 0' }} {...props} />
          ),
          strong: ({node, ...props}) => (
            <strong style={{ color: '#111827', fontWeight: '600' }} {...props} />
          ),
          em: ({node, ...props}) => (
            <em style={{ color: '#374151' }} {...props} />
          ),
          code: ({node, ...props}) => (
            <code style={{ 
              color: '#1f2937', 
              backgroundColor: '#f3f4f6', 
              padding: '2px 4px', 
              borderRadius: '4px',
              fontSize: '0.9rem'
            }} {...props} />
          ),
          pre: ({node, ...props}) => (
            <pre style={{ 
              color: '#1f2937', 
              backgroundColor: '#f3f4f6', 
              padding: '12px', 
              borderRadius: '8px',
              overflow: 'auto',
              margin: '0 0 16px 0'
            }} {...props} />
          ),
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
};

export default MarkdownViewer;