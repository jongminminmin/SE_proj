import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../App';
import styles from './Main.module.css';
import { Pencil, Trash2 } from 'lucide-react';

function Main() {
  const [tab, setTab] = useState('all');
  const [projectOpen, setProjectOpen] = useState(true);
  const [showProjectModal, setShowProjectModal] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [editProjectId, setEditProjectId] = useState(null);
  const [newProject, setNewProject] = useState({
    projectTitle: '',
    description: '',
    date: '',
  });
  const [projects, setProjects] = useState([]);
  const navigate = useNavigate();
  const { currentUser } = useAuth();

  // 프로젝트 목록 불러오기
  const fetchProjects = async () => {
    const res = await fetch('/api/projects');
    const data = await res.json();
    setProjects(data);
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  // 로그아웃 함수 정의
  const handleLogout = async () => {
    console.log('Logout process started...'); // 로그: 로그아웃 시작

    // 1. 로컬 스토리지에서 토큰 제거
    try {
      localStorage.removeItem('token');
      console.log('Token removed from localStorage.');
    } catch (e) {
      console.error('Error removing token from localStorage:', e);
    }

    // 2. 서버 측 로그아웃 API 호출 (Spring Security의 /logout은 보통 POST)
    try {
      const response = await fetch('/logout', { // Spring Security의 기본 로그아웃 URL
        method: 'POST',
        // Spring Security와 세션 쿠키를 사용한다면 credentials 옵션이 필요할 수 있습니다.
        // credentials: 'include',
        // CSRF 보호가 활성화되어 있다면, CSRF 토큰을 헤더에 포함해야 합니다.
        // headers: {
        //   'X-CSRF-TOKEN': csrfToken, // CSRF 토큰을 가져오는 방식에 따라 다름
        // },
      });

      if (response.ok) {
        // 서버 로그아웃 성공 (HTTP 상태 코드 2xx)
        console.log('Server logout successful. Status:', response.status, 'Redirected:', response.redirected);
      } else {
        // 서버 로그아웃 실패 (HTTP 상태 코드가 2xx가 아님)
        console.warn('Server logout failed. Status:', response.status);
      }
    } catch (error) {
      // 네트워크 오류 등으로 서버 로그아웃 API 호출 자체가 실패한 경우
      console.error('Error during server logout API call:', error);
    } finally {
      // 3. 로그인 페이지로 리디렉션 (서버 응답과 관계없이 항상 실행되도록 finally에 위치)
      console.log('Navigating to /login page.');
      navigate('/login');
    }
  };

  // ESLint: 'filteredProjects' is defined here.
  // 이 변수는 현재 선택된 탭에 따라 projectList를 필터링합니다.
  const filteredProjects =
      tab === 'all'
          ? projects
          : projects.filter((p) => p.status === tab);

  // 알림 더미 데이터
  const notifications = [
    { id: 1, text: '새로운 할 일이 등록되었습니다.' },
    { id: 2, text: '프로젝트 베타의 마감일이 다가옵니다.' },
    { id: 3, text: '팀원이 댓글을 남겼습니다.' },
  ];

  // 프로젝트 추가/수정
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
      // 수정
      await fetch('/api/projects', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...dto, projectId: editProjectId })
      });
    } else {
      // 추가
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
    fetchProjects();
  };

  // 삭제
  const handleDeleteProject = async (projectId) => {
    if (!window.confirm('정말 삭제하시겠습니까?')) return;
    await fetch(`/api/projects/${projectId}`, { method: 'DELETE' });
    fetchProjects();
  };

  // 수정 모달 열기
  const openEditModal = (project) => {
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
      <aside className={styles.sidebar}>
        <div className={styles.sidebarTitle}>MATE</div>
        <ul className={styles.sidebarMenu}>
          <li className={`${styles.sidebarMenuItem} ${styles.active}`} onClick={() => setProjectOpen((v) => !v)}>
            <span role="img" aria-label="project" className={styles.icon}>📋</span> Project
          </li>
          {projectOpen && (
            <ul className={styles.sidebarSubMenu}>
              <li className={styles.sidebarSubMenuItem} onClick={() => navigate('/task')}>
                <span role="img" aria-label="task" className={styles.iconSub}>📝</span> Task
              </li>
            </ul>
          )}
          <li className={styles.sidebarMenuItem} onClick={() => navigate('/chat')}>
            <span role="img" aria-label="chat" className={styles.icon}>💬</span> Chat
          </li>
          <li className={styles.sidebarMenuItem}>
            <span role="img" aria-label="settings" className={styles.icon}>⚙️</span> Settings
          </li>
        </ul>
      </aside>
      <main className={styles.mainContent}>
        {/* 상단 헤더에 로그아웃 버튼 추가 */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', marginBottom: '16px' }}>
          <button onClick={handleLogout} className={styles.logoutBtn} style={{ padding: '8px 16px', background: '#007aff', color: 'white', border: 'none', borderRadius: '6px', fontWeight: 500, fontSize: '14px', cursor: 'pointer', height: '40px' }}>로그아웃</button>
        </div>
        <div className={styles.splitLayout}>
          {/* 왼쪽: 프로젝트 리스트 */}
          <div className={styles.projectListSection}>
            <h2 className={styles.sectionTitle}>프로젝트 목록</h2>
            <ul className={styles.projectList}>
              {projects.map((p, i) => (
                <li
                  key={p.projectId || i}
                  className={styles.projectListItem}
                  onClick={() => navigate(`/task?project=${encodeURIComponent(p.projectTitle)}`)}
                >
                  <span className={styles.projectDot} style={{backgroundColor: p.color || '#007aff'}}></span>
                  {p.projectTitle}
                  <button
                    className={`${styles.projectActionBtn} ${styles.projectEditBtn}`}
                    title="수정"
                    onClick={e => {e.stopPropagation(); openEditModal(p);}}
                  >
                    <Pencil size={16} style={{marginRight: 2}} /> 수정
                  </button>
                  <button
                    className={`${styles.projectActionBtn} ${styles.projectDeleteBtn}`}
                    title="삭제"
                    onClick={e => {e.stopPropagation(); handleDeleteProject(p.projectId);}}
                  >
                    <Trash2 size={16} style={{marginRight: 2}} /> 삭제
                  </button>
                </li>
              ))}
            </ul>
            {/* 프로젝트 추가 문구 */}
            <div
              className={styles.addProjectText}
              style={{ color: '#2563eb', marginTop: '16px', cursor: 'pointer', fontWeight: 500 }}
              onClick={() => { setShowProjectModal(true); setEditMode(false); setNewProject({ projectTitle: '', description: '', date: '' }); }}
            >
              + 프로젝트 추가
            </div>
          </div>
          {/* 오른쪽: 알림 */}
          <div className={styles.rightSection}>
            <div className={styles.notificationBox}>
              <h3 className={styles.notificationTitle}>알림</h3>
              <ul className={styles.notificationList}>
                {notifications.map(n => (
                  <li key={n.id} className={styles.notificationItem}>{n.text}</li>
                ))}
              </ul>
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
