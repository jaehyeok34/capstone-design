import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Home from "./pages/Home";
import Analysis from "./pages/Analysis";
import Join from "./pages/join";
import NavBar from "./components/NavBar";

function App() {
  return (
    <Router>
      {/* 상단 고정 네비게이션 */}
      <NavBar />

      {/* 메인 컨텐츠 (네비게이션 높이만큼 padding 추가) */}
      <div style={{ 
        paddingTop: '100px',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        width: '100vw',
        minHeight: 'calc(100vh - 80px)',
        position: 'relative',
        left: '50%',
        right: '50%',
        marginLeft: '-50vw',
        marginRight: '-50vw'
      }}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/analysis" element={<Analysis />} />
          <Route path="/join" element={<Join />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;