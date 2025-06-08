import { useEffect, useRef, useState } from 'react';
import styles from './DatasetList.module.css';
import DataDialog from './DataDialog';

interface DatasetItem {
  type: string;
  datasetInfo: string;
  dbConnectionInfo: string | null;
}

function truncateText(text: string, maxLength: number) {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength - 3) + '...';
}

function splitDatasets(data: DatasetItem[]) {
  const mk = data.filter((d) => d.datasetInfo.startsWith('mk_'));
  const matched = data.filter((d) => d.datasetInfo.startsWith('matched_'));
  const merged = data.filter((d) => d.datasetInfo.startsWith('merged_'));
  const pseudonymized = data.filter((d) => d.datasetInfo.startsWith('psudonymized_'));
  const normal = data.filter(
    (d) =>
      !d.datasetInfo.startsWith('mk_') &&
      !d.datasetInfo.startsWith('matched_') &&
      !d.datasetInfo.startsWith('merged_') &&
      !d.datasetInfo.startsWith('psudonymized_')
  );
  return { mk, matched, merged, pseudonymized, normal };
}

export default function DatasetList() {
  const [data, setData] = useState<DatasetItem[]>([]);
  const [uploading, setUploading] = useState(false);

  // dialog 상태
  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogDatasetInfo, setDialogDatasetInfo] = useState<string | null>(null);
  const [dialogData, setDialogData] = useState<Record<string, any[]> | null>(null);
  const [dialogLoading, setDialogLoading] = useState(false);
  const [dialogError, setDialogError] = useState<string | null>(null);

  // 결합 완료 dialog 상태
  const [mergeDialogOpen, setMergeDialogOpen] = useState(false);
  const [mergeDialogDatasets, setMergeDialogDatasets] = useState<string[]>([]);
  // 결합 후 빠른 폴링 상태
  const [isMerging, setIsMerging] = useState(false);

  // 파일 업로드 input ref 직접 관리
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 최소 선택 안내 dialog 상태
  const [minSelectDialogOpen, setMinSelectDialogOpen] = useState(false);

  // 서버 연결 에러 dialog 상태 제거
  // const [networkErrorDialogOpen, setNetworkErrorDialogOpen] = useState(false);

  // 데이터셋 목록 갱신 (에러 시 빈 목록만 보여줌)
  const fetchDatasets = () => {
    fetch('/data/datasets')
      .then((res) => {
        if (!res.ok) throw new Error('네트워크 오류');
        return res.json();
      })
      .then((newData) => {
        if (JSON.stringify(newData) !== JSON.stringify(data)) {
          setData(newData);
        }
      })
      .catch(() => {
        setData([]); // 에러 시 빈 목록만 보여줌
      });
  };

  useEffect(() => {
    fetchDatasets();
  }, []);

  // 폴링 주기 상태
  const [pollingInterval, setPollingInterval] = useState(5000);

  // 주기적 데이터 갱신 (pollingInterval마다)
  useEffect(() => {
    const interval = setInterval(() => {
      fetchDatasets();
    }, pollingInterval);
    return () => clearInterval(interval);
  }, [pollingInterval]);

  // 업로드 버튼 클릭 핸들러
  const handleUploadButtonClick = () => {
    fileInputRef.current?.click();
  };

  // 클릭 핸들러 (다중 선택, log 제거)
  const handleClick = (group: string, idx: number, item: DatasetItem) => {
    setSelectedItems((prev: { group: string; idx: number; item: DatasetItem }[]) => {
      const hasOtherGroup = prev.length > 0 && prev[0].group !== group;
      let next;
      if (hasOtherGroup) {
        next = [{ group, idx, item }];
      } else {
        const exists = prev.find((sel: { group: string; idx: number; item: DatasetItem }) => sel.group === group && sel.idx === idx);
        if (exists) {
          next = prev.filter((sel: { group: string; idx: number; item: DatasetItem }) => !(sel.group === group && sel.idx === idx));
        } else {
          next = [...prev, { group, idx, item }];
        }
      }
      return next;
    });
  };

  // 더블클릭 핸들러
  const handleDoubleClick = async (datasetInfo: string) => {
    setDialogOpen(true);
    setDialogDatasetInfo(datasetInfo);
    setDialogData(null);
    setDialogError(null);
    setDialogLoading(true);
    try {
      const res = await fetch(`/csv/all-values/${encodeURIComponent(datasetInfo)}`);
      if (!res.ok) throw new Error('데이터를 불러오지 못했습니다');
      const json = await res.json();
      setDialogData(json);
    } catch (e: any) {
      setDialogError(e.message || '에러 발생');
    } finally {
      setDialogLoading(false);
    }
  };

  const handleDialogClose = () => {
    setDialogOpen(false);
    setDialogDatasetInfo(null);
    setDialogData(null);
    setDialogError(null);
    setDialogLoading(false);
  };

  // 결합 버튼 클릭 핸들러 (원본 데이터군만 동작, 최소 2개 체크, POST만)
  const handleMergeClick = async () => {
    setSelectedItems([]); // 결합 버튼 누를 때 선택 해제
    const normalSelected = selectedItems.filter((sel: { group: string; idx: number; item: DatasetItem }) => sel.group === 'normal');
    if (normalSelected.length < 2) {
      setMinSelectDialogOpen(true);
      setTimeout(() => setMinSelectDialogOpen(false), 1000);
      return;
    }
    const datasetInfos = normalSelected.map((sel: { group: string; idx: number; item: DatasetItem }) => sel.item.datasetInfo);
    await fetch('/event/publish', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'pii.detection.request', jsonData: JSON.stringify(datasetInfos) })
    });
    setMergeDialogDatasets(datasetInfos);
    setMergeDialogOpen(true);
    setIsMerging(true);
    setPollingInterval(1000); // 결합 후 1초마다 빠른 폴링
  };

  // 결합 dialog가 열려 있을 때만 빠른 폴링, merged_ 데이터가 갱신될 때 dialog 닫기
  useEffect(() => {
    if (!isMerging) return;
    // merged_ 데이터셋만 추출
    let prevMerged = data.filter((d) => d.datasetInfo.startsWith('merged_'));
    const prevMergedStr = JSON.stringify(prevMerged);
    const interval = setInterval(() => {
      fetch('/data/datasets')
        .then((res) => {
          if (!res.ok) throw new Error('네트워크 오류');
          return res.json();
        })
        .then((newData) => {
          const newMerged = newData.filter((d: DatasetItem) => d.datasetInfo.startsWith('merged_'));
          const newMergedStr = JSON.stringify(newMerged);
          if (newMergedStr !== prevMergedStr) {
            setData(newData);
            setTimeout(() => {
              setMergeDialogOpen(false);
              setMergeDialogDatasets([]);
              setIsMerging(false);
              setPollingInterval(5000); // 다시 5초로 복구
            }, 0);
          }
        })
        .catch(() => {});
    }, 1000); // 1초마다
    return () => clearInterval(interval);
  }, [isMerging, data]);

  // 에러 메시지 대신 네트워크 에러 dialog
  // if (error) return <div className={styles.error}>에러: {error}</div>;

  const { mk, matched, merged, pseudonymized, normal } = splitDatasets(data);

  // 탭 정보
  const tabList = [
    { key: 'normal', label: '원본', items: normal },
    { key: 'mk', label: '결합키', items: mk },
    { key: 'matched', label: '연계정보', items: matched },
    { key: 'merged', label: '결합정보', items: merged },
    { key: 'pseudonymized', label: '가명정보', items: pseudonymized },
  ] as const;

  // 다중 선택된 데이터셋 인덱스 상태 (타입 명시)
  const [selectedItems, setSelectedItems] = useState<{ group: string; idx: number; item: DatasetItem }[]>([]);

  // 탭 상태 (5개 데이터군)
  const [activeTab, setActiveTab] = useState<'normal' | 'mk' | 'matched' | 'merged' | 'pseudonymized'>('normal');

  // 데이터군 외 영역 클릭 시 포커싱 해제
  const handleContainerClick = (e: React.MouseEvent<HTMLDivElement>) => {
    // 데이터군 요소(.datasetCard) 클릭이 아니면 선택 해제
    if (!(e.target as HTMLElement).closest('.' + styles.datasetCard)) {
      setSelectedItems([]);
    }
  };

  // renderList 함수 정의를 return문 위로 이동
  const renderList = (items: DatasetItem[], groupKey: string) => (
    <div style={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <ul className={styles.scrollList + ' ' + styles.fixedScrollList}>
        {items.length === 0 ? (
          <li style={{ color: '#888', padding: '1rem', textAlign: 'center' }}>데이터 없음</li>
        ) : (
          items.map((item, idx) => {
            const selected = selectedItems.some((sel) => sel.group === groupKey && sel.idx === idx);
            return (
              <li
                key={idx}
                className={styles.datasetCard}
                onClick={() => handleClick(groupKey, idx, item)}
                onDoubleClick={() => handleDoubleClick(item.datasetInfo)}
                style={{
                  cursor: 'pointer',
                  outline: selected ? '2px solid #7ab7ff' : undefined,
                  boxShadow: selected ? '0 0 0 2px #7ab7ff' : undefined,
                  background: selected ? '#1a2330' : undefined,
                }}
              >
                <span
                  className={styles.datasetTitle + ' ' + styles.ellipsis}
                  title={item.datasetInfo}
                >
                  {truncateText(item.datasetInfo, 50)}
                </span>
                <span className={styles.datasetType}>
                  Type: <span>{item.type}</span>
                </span>
                {item.dbConnectionInfo && (
                  <span className={styles.datasetDbInfo}>
                    DB Info: <span>{item.dbConnectionInfo}</span>
                  </span>
                )}
              </li>
            );
          })
        )}
      </ul>
    </div>
  );

  // pseudonymizeErrorDialogOpen, setPseudonymizeErrorDialogOpen 상태를 useState들과 함께 최상단에 선언
  const [pseudonymizeErrorDialogOpen, setPseudonymizeErrorDialogOpen] = useState(false);
  // 가명처리 완료 dialog 상태
  const [pseudonymizeSuccessDialogOpen, setPseudonymizeSuccessDialogOpen] = useState(false);

  return (
    <div className={styles.container + ' ' + styles.wideContainer} onClick={handleContainerClick}>
      <h1 className={styles.title} style={{ textAlign: 'center' }}>데이터셋 목록</h1>
      <div style={{ display: 'flex', gap: '1.2rem', marginBottom: '1.5rem', justifyContent: 'center' }}>
        {tabList.map(tab => (
          <button
            key={tab.key}
            type="button"
            onClick={e => { e.stopPropagation(); setActiveTab(tab.key); }}
            style={{
              background: activeTab === tab.key ? '#181b20' : '#23272f',
              color: activeTab === tab.key ? '#7ab7ff' : '#b0b6c2',
              border: activeTab === tab.key ? '2px solid #7ab7ff' : '1px solid #2c313a',
              borderRadius: 8,
              padding: '0.7rem 2.2rem',
              fontWeight: 700,
              fontSize: '1.08rem',
              cursor: 'pointer',
              boxShadow: activeTab === tab.key ? '0 2px 8px #0004' : undefined,
              transition: 'all 0.15s',
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>
      <div className={styles.datasetTabContent}>
        {tabList.map(tab => (
          activeTab === tab.key && (
            <div key={tab.key}>
              {renderList(tab.items, tab.key)}
            </div>
          )
        ))}
      </div>
      <div style={{ display: 'flex', gap: '1rem', marginTop: '2.5rem', justifyContent: 'flex-end', alignItems: 'center', width: '100%' }}>
        <button
          type="button"
          style={{
            background: '#23272f',
            color: '#7ab7ff',
            border: '1px solid #7ab7ff',
            borderRadius: 6,
            padding: '0.6rem 1.8rem',
            fontWeight: 600,
            fontSize: '1rem',
            cursor: 'pointer',
            minWidth: 120,
            height: '48px',
            boxSizing: 'border-box',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
          onClick={async () => {
            // 결합키, 연계정보 탭에서 선택된 경우 에러 dialog
            if (activeTab === 'mk' || activeTab === 'matched') {
              setPseudonymizeErrorDialogOpen(true);
              setTimeout(() => setPseudonymizeErrorDialogOpen(false), 2000);
              setSelectedItems([]);
              return;
            }
            if (selectedItems.length > 0) {
              setPseudonymizeSuccessDialogOpen(true);
              setPollingInterval(2000); // 2초로 폴링 주기 변경
              for (const sel of selectedItems) {
                await fetch('/event/publish', {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({
                    name: 'pseudonymization.pseudonymize.request', // 오타 수정
                    pathVariable: sel.item.datasetInfo,
                  })
                });
              }
              setSelectedItems([]);
              setTimeout(() => {
                setPseudonymizeSuccessDialogOpen(false);
              }, 2000);
              setIsMerging(true); // 빠른 폴링 시작 (merged_ 갱신 감지)
            }
          }}
        >
          가명처리
        </button>
        <button
          type="button"
          style={{
            background: '#23272f',
            color: '#7ab7ff',
            border: '1px solid #7ab7ff',
            borderRadius: 6,
            padding: '0.6rem 1.8rem',
            fontWeight: 600,
            fontSize: '1rem',
            cursor: 'pointer',
            minWidth: 120,
            height: '48px',
            boxSizing: 'border-box',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
          onClick={handleMergeClick}
        >
          결합
        </button>
        <button
          type="button"
          style={{
            background: '#23272f',
            color: '#7ab7ff',
            border: '1px solid #7ab7ff',
            borderRadius: 6,
            padding: '0.6rem 1.8rem',
            fontWeight: 600,
            fontSize: '1rem',
            cursor: uploading ? 'not-allowed' : 'pointer',
            minWidth: 120,
            height: '48px',
            boxSizing: 'border-box',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            opacity: uploading ? 0.6 : 1,
          }}
          onClick={handleUploadButtonClick}
          disabled={uploading}
        >
          {uploading ? '업로드 중...' : '데이터 등록'}
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept=".csv"
          style={{ display: 'none' }}
          onChange={async (e) => {
            const file = e.target.files?.[0];
            if (file) {
              setUploading(true);
              const formData = new FormData();
              formData.append('file', file);
              try {
                const res = await fetch('/csv/register', {
                  method: 'POST',
                  body: formData,
                });
                if (!res.ok) throw new Error('업로드 실패');
                fetchDatasets();
              } catch (err: any) {
                alert(err.message || '업로드 중 오류 발생');
              } finally {
                setUploading(false);
              }
              e.target.value = '';
            }
          }}
        />
      </div>
      <DataDialog
        open={dialogOpen}
        onClose={handleDialogClose}
        datasetInfo={dialogDatasetInfo}
        data={dialogData}
        loading={dialogLoading}
        error={dialogError}
      />
      {minSelectDialogOpen && (
        <div style={{
          position: 'fixed', left: 0, top: 0, width: '100vw', height: '100vh',
          background: 'rgba(0,0,0,0.25)', zIndex: 2000, display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <div style={{
            background: '#23272f', color: '#fff', borderRadius: 10, padding: '2rem 2.5rem', fontSize: '1.2rem', fontWeight: 600,
            boxShadow: '0 4px 24px #0008', minWidth: 220, textAlign: 'center'
          }}>
            최소 2개 선택해 주세요
          </div>
        </div>
      )}
      {mergeDialogOpen && (
        <div
          style={{
            position: 'fixed', left: 0, top: 0, width: '100vw', height: '100vh',
            background: 'rgba(0,0,0,0.25)', zIndex: 2100, display: 'flex', alignItems: 'center', justifyContent: 'center'
          }}
          onClick={() => {
            setMergeDialogOpen(false);
            setMergeDialogDatasets([]);
            setIsMerging(false);
            setPollingInterval(5000);
          }}
        >
          <div
            style={{
              background: '#23272f', color: '#fff', borderRadius: 12, padding: '2.2rem 2.7rem', fontSize: '1.1rem', fontWeight: 500,
              boxShadow: '0 4px 24px #0008', minWidth: 320, textAlign: 'center', maxWidth: 420, border: '2px solid #7ab7ff'
            }}
            onClick={e => e.stopPropagation()}
          >
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '1.2rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.7rem', fontWeight: 700, fontSize: '1.25rem', color: '#7ab7ff' }}>
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#7ab7ff" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="9 12 12 15 17 10"/></svg>
                결합 신청 완료
              </div>
              <div style={{ color: '#b0b6c2', fontSize: '1.08rem', fontWeight: 600 }}>
                아래 데이터셋으로 결합을 신청했습니다
              </div>
              <div style={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.3rem' }}>
                {mergeDialogDatasets.map((info, i) => (
                  <div key={i} style={{ background: '#181b20', borderRadius: 6, padding: '0.6rem 1.2rem', color: '#e0e6f0', fontSize: '1.01rem', wordBreak: 'break-all', width: '100%', textAlign: 'center', border: '1px solid #2c313a' }}>{info}</div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
      {pseudonymizeErrorDialogOpen && (
        <div style={{
          position: 'fixed', left: 0, top: 0, width: '100vw', height: '100vh',
          background: 'rgba(0,0,0,0.25)', zIndex: 2200, display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <div style={{
            background: '#23272f', color: '#fff', borderRadius: 12, padding: '2.2rem 2.7rem', fontSize: '1.15rem', fontWeight: 600,
            boxShadow: '0 4px 24px #0008', minWidth: 260, textAlign: 'center', maxWidth: 400, border: '2px solid #ff5555'
          }}>
            해당 정보는 가명처리 불가능합니다
          </div>
        </div>
      )}
      {pseudonymizeSuccessDialogOpen && (
        <div style={{
          position: 'fixed', left: 0, top: 0, width: '100vw', height: '100vh',
          background: 'rgba(0,0,0,0.25)', zIndex: 2200, display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <div style={{
            background: '#23272f', color: '#fff', borderRadius: 12, padding: '2.2rem 2.7rem', fontSize: '1.15rem', fontWeight: 600,
            boxShadow: '0 4px 24px #0008', minWidth: 260, textAlign: 'center', maxWidth: 400, border: '2px solid #7ab7ff'
          }}>
            가명처리 요청을 보냈습니다
          </div>
        </div>
      )}
    </div>
  );
}
