import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useAuth} from '../App';
import styles from './Main.module.css';
import {Folder, MessageCircle, Settings, Pencil, Trash2} from 'lucide-react'; // Pencil, Trash2 아이콘 추가
import Calendar from 'react-calendar';
import 'react-calendar/dist/Calendar.css';
import defaultProfile from '../assets/default-profile.png'; // 기본 프로필 이미지 경로(없으면 public 경로 사용)

function Main() {
  const [tab, setTab] = useState('all'); // setTab은 현재 사용되지 않지만, 탭 기능을 위해 유지
  // const [projectOpen, setProjectOpen] = useState(true); // 현재 사용되지 않아 주석 처리
  // const [setProjectOpen] = useState(true); // 현재 사용되지 않아 주석 처리
  const [showProjectModal, setShowProjectModal] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [editProjectId, setEditProjectId] = useState(null);
  const [newProject, setNewProject] = useState({
    projectTitle: '',
    description: '',
    date: '',
  });
  const [projects, setProjects] = useState([]);
  const [calendarValue, setCalendarValue] = useState(new Date());
  const navigate = useNavigate();
  const { currentUser } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(true);

  // 프로젝트 목록 불러오기
  const fetchProjects = async () => {
    const res = await fetch('/api/projects');
    const data = await res.json();
    setProjects(data);
  };

  useEffect(() => {
    fetchProjects().catch(console.error);
  }, []);

  // 로그아웃 함수 정의
  const handleLogout = async () => {
    localStorage.removeItem('token');
    try {
      await fetch('/logout', { method: 'POST' });
    } catch {}
    navigate('/login');
  };

  const filteredProjects = tab === 'all' ? projects : projects.filter((p) => p.status === tab); // 사용 중

  const notifications = [
    { id: 1, text: '새로운 할 일이 등록되었습니다.' },
    { id: 2, text: '프로젝트 베타의 마감일이 다가옵니다.' },
    { id: 3, text: '팀원이 댓글을 남겼습니다.' },
  ];

  const handleAddOrEditProject = async () => {
    if (!newProject.projectTitle.trim() || !newProject.date) {
      alert('프로젝트 명과 마감일은 필수입니다.');
      return;
    }
    const dto = {
      projectTitle: newProject.projectTitle,
      description: newProject.description,
      date: newProject.date,
      ownerId: currentUser?.id || 'admin',
      projectMemberTier: 'member',
    };
    if (editMode && editProjectId) {
      await fetch('/api/projects', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...dto, projectId: editProjectId })
      });
    } else {
      await fetch('/api/projects', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(dto)
      });
    }
    setShowProjectModal(false);
    setEditMode(false);
    setEditProjectId(null);
    setNewProject({ projectTitle: '', description: '', date: '' });
    await fetchProjects();
  };

  const handleDeleteProject = async (projectId) => { // 사용되도록 수정
    if (!window.confirm('정말 삭제하시겠습니까?')) return;
    await fetch(`/api/projects/${projectId}`, { method: 'DELETE' });
    await fetchProjects();
  };

  const openEditModal = (project) => { // 사용되도록 수정
    setEditMode(true);
    setEditProjectId(project.projectId);
    setNewProject({
      projectTitle: project.projectTitle,
      description: project.description,
      date: project.date ? project.date.slice(0, 10) : '',
    });
    setShowProjectModal(true);
  };

  return (
      <div className={styles.wrapper}>
        {/* 사이드바 */}
        <aside className={styles.sidebar} style={{ width: sidebarOpen ? 240 : 56, transition: 'width 0.2s' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: sidebarOpen ? 'space-between' : 'center', padding: '0 12px 0 8px', marginBottom: 24 }}>
            {sidebarOpen && <div className={styles.sidebarTitle}>MATE</div>}
            <button onClick={() => setSidebarOpen(v => !v)} style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 4 }}>
              <span style={{ fontSize: 20 }}>{sidebarOpen ? '<' : '>'}</span>
            </button>
          </div>
          <ul className={styles.sidebarMenu} style={{ alignItems: sidebarOpen ? 'flex-start' : 'center' }}>
            <li className={styles.sidebarMenuItem}>
              <Folder size={22} style={{ marginRight: sidebarOpen ? 12 : 0 }} />
              {sidebarOpen && 'Project'}
            </li>
            <li className={styles.sidebarMenuItem} onClick={() => navigate('/chat')}>
              <MessageCircle size={22} style={{ marginRight: sidebarOpen ? 12 : 0 }} />
              {sidebarOpen && 'Chat'}
            </li>
            {/* Settings 메뉴 항목에 navigate('/settings') 추가 */}
            <li className={styles.sidebarMenuItem} onClick={() => navigate('/Settings')}>
              <Settings size={22} style={{ marginRight: sidebarOpen ? 12 : 0 }} />
              {sidebarOpen && 'Settings'}
            </li>
          </ul>
        </aside>
        {/* 메인 컨텐츠 */}
        <main className={styles.mainContent}>
          {/* 상단 바 */}
          <div className={styles.header}>
            <div className={styles.titleBox}>
              <span className={styles.title}>프로젝트</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
              <img
                  src={defaultProfile}
                  alt="프로필"
                  style={{ width: 40, height: 40, borderRadius: '50%', objectFit: 'cover', background: '#e5e7eb', border: '1.5px solid #d1d5db' }}
              />
              <button onClick={handleLogout} className={styles.logoutBtn} style={{ marginLeft: 8 }}>로그아웃</button>
            </div>
          </div>
          {/* 본문 영역: 좌우 분할 */}
          <div className={styles.splitLayout}>
            {/* 좌측: 프로젝트 표 */}
            <div className={styles.projectListSection} style={{ minWidth: 350, maxWidth: 600, flex: 2 }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', background: 'white', borderRadius: 8, overflow: 'hidden', boxShadow: '0 1px 4px rgba(0,0,0,0.04)' }}>
                <thead>
                <tr style={{ background: '#f5f7fa', color: '#333', fontWeight: 600, fontSize: 15 }}>
                  <th style={{ padding: '12px 8px', textAlign: 'left' }}>이름</th>
                  <th style={{ padding: '12px 8px', textAlign: 'left' }}>프로젝트 내용</th>
                  <th style={{ padding: '12px 8px', textAlign: 'left' }}>리더</th>
                  <th style={{ padding: '12px 8px', textAlign: 'left' }}>액션</th> {/* 액션 컬럼 추가 */}
                </tr>
                </thead>
                <tbody>
                {projects.map((p, i) => (
                    <tr
                        key={p.projectId || i}
                        style={{ cursor: 'pointer', borderBottom: '1px solid #f0f0f0', transition: 'background 0.15s' }}
                        onClick={() => navigate(`/task?project=${encodeURIComponent(p.projectTitle)}&projectId=${encodeURIComponent(p.projectId)}`)}
                    >
                      <td style={{ padding: '10px 8px', fontWeight: 500 }}>
                        <span className={styles.projectDot} style={{ backgroundColor: p.color || '#007aff', marginRight: 8 }}></span>
                        {p.projectTitle}
                      </td>
                      <td style={{ padding: '10px 8px', color: '#555', fontSize: 14 }}>{p.description || '-'}</td>
                      <td style={{ padding: '10px 8px' }}>{p.leader || currentUser?.name || '관리자'}</td>
                      <td style={{ padding: '10px 8px' }}>
                        <button onClick={(e) => { e.stopPropagation(); openEditModal(p); }} style={{ background: 'none', border: 'none', cursor: 'pointer', marginRight: 8 }}>
                          <Pencil size={18} color="#2563eb" />
                        </button>
                        <button onClick={(e) => { e.stopPropagation(); handleDeleteProject(p.projectId); }} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                          <Trash2 size={18} color="#dc2626" />
                        </button>
                      </td>
                    </tr>
                ))}
                </tbody>
              </table>
              {/* + 프로젝트 추가 문구 */}
              <div
                  className={styles.addProjectText}
                  style={{ color: '#2563eb', marginTop: '16px', cursor: 'pointer', fontWeight: 500 }}
                  onClick={() => { setShowProjectModal(true); setEditMode(false); setNewProject({ projectTitle: '', description: '', date: '' }); }}
              >
                + 프로젝트 추가
              </div>
            </div>
            {/* 우측: 알림 + 캘린더 */}
            <div className={styles.rightSection}>
              <div className={styles.notificationBox}>
                <h3 className={styles.notificationTitle}>알림</h3>
                <ul className={styles.notificationList}>
                  {notifications.map(n => (
                      <li key={n.id} className={styles.notificationItem}>{n.text}</li>
                  ))}
                </ul>
              </div>
              <div className={styles.calendarBox} style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'stretch', height: '100%' }}>
                <Calendar
                    onChange={setCalendarValue}
                    value={calendarValue}
                    className={styles.reactCalendar}
                    locale="ko-KR"
                    style={{ flex: 1, width: '100%', height: '100%' }}
                    tileContent={null}
                />
              </div>
            </div>
          </div>
        </main>
        {/* 프로젝트 추가/수정 모달 */}
        {showProjectModal && (
            <div className={styles.modalOverlay}>
              <div className={styles.modal}>
                <h3 className={styles.modalTitle}>{editMode ? '프로젝트 수정' : '프로젝트 추가'}</h3>
                <div className={styles.formContainer}>
                  <input
                      type="text"
                      placeholder="프로젝트 명"
                      value={newProject.projectTitle}
                      onChange={e => setNewProject({ ...newProject, projectTitle: e.target.value })}
                      className={styles.formInput}
                  />
                  <textarea
                      placeholder="프로젝트 내용"
                      value={newProject.description}
                      onChange={e => setNewProject({ ...newProject, description: e.target.value })}
                      className={styles.formTextarea}
                  />
                  <input
                      type="date"
                      value={newProject.date}
                      onChange={e => setNewProject({ ...newProject, date: e.target.value })}
                      className={styles.formInput}
                  />
                </div>
                <div className={styles.modalButtons}>
                  <button
                      className={styles.btnSecondary}
                      onClick={() => { setShowProjectModal(false); setEditMode(false); setEditProjectId(null); }}
                  >취소</button>
                  <button
                      className={styles.btnPrimary}
                      onClick={handleAddOrEditProject}
                  >{editMode ? '수정 완료' : '프로젝트 추가'}</button>
                </div>
              </div>
            </div>
        )}
      </div>
  );
}

export default Main;
