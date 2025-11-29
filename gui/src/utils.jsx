import checkIcon from './assets/illustration/check.png';
import sendwatchIcon from './assets/illustration/sendwatch.png';
import pauseIcon from './assets/illustration/pause.png';
import { saveAs } from 'file-saver';

const getStatusColor = (status) => {
    return {
      "idle": "#6b7280",
      "active": "#f59e0b",
      "done": "#10b981",
      // "failed": "#ef4444"
    }[status] || "#6b7280";
  };

const getStatusText = (status) => {
    return {
        "idle": "대기중",
        "active": "진행중",
        "done": "완료",
        // "failed": "실패"
    }[status] || "알 수 없음";
};

const getStatusIcon = (status) => {
    return {
        "idle": <img src={pauseIcon} alt="idle" style={{ width: 16, height: 16 }} />,
        "active": <img src={sendwatchIcon} alt="active" style={{ width: 16, height: 16 }} />,
        "done": <img src={checkIcon} alt="done" style={{ width: 16, height: 16 }} />,
        "failed": '❌'
    }[status] || '❓';
};

const downloadHandler = async (projectId, projectName) => {
    const response = await fetch(`http://localhost:8000/api/result/${projectId}`);
    if (!response.ok) {
        alert(`결과 파일 다운로드 실패: ${response.status}`);
        return;
    }

    saveAs(await response.blob(), `${projectName}_result.csv`)
};

export { getStatusColor, getStatusText, getStatusIcon, downloadHandler };