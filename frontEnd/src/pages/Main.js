import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useAuth} from '../App';
import styles from './Main.module.css';
import Calendar from 'react-calendar';
import 'react-calendar/dist/Calendar.css';
import defaultProfile from '../assets/default-profile.png';
import { useNotifications } from '../NotificationContext';

const Folder = ({ size, style }) => <svg style={style} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path></svg>;
const MessageCircle = ({ size, style }) => <svg style={style} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path></svg>;
const Settings = ({ size, style }) => <svg style={style} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>;
const Pencil = ({ size, color }) => <svg width={size} height={size} color={color} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path></svg>;
const Trash2 = ({ size, color }) => <svg width={size} height={size} color={color} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>;

function Main() {
  const [tab, setTab] = useState('all');
  const [showProjectModal, setShowProjectModal] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [editProjectId, setEditProjectId] = useState(null);
  const [newProject, setNewProject] = useState({ projectTitle: '', description: '', date: '' });
  const [projects, setProjects] = useState([]);
  const [calendarValue, setCalendarValue] = useState(new Date());
  const navigate = useNavigate();
  const { currentUser, logout } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const { globalUnreadCount, updateGlobalUnreadCount } = useNotifications();

  useEffect(() => {
    updateGlobalUnreadCount();
    const handleFocus = () => updateGlobalUnreadCount();
    window.addEventListener('focus', handleFocus);
    return () => window.removeEventListener('focus', handleFocus);
  }, [updateGlobalUnreadCount]);

  const fetchProjects = async () => {
    const res = await fetch('/api/projects');
    const data = await res.json();
    setProjects(data);
  };

  useEffect(() => {
    fetchProjects().catch(console.error);
  }, []);

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

  const handleDeleteProject = async (projectId) => {
    if (!window.confirm('정말 삭제하시겠습니까?')) return;
    await fetch(`/api/projects/${projectId}`, { method: 'DELETE' });
    await fetchProjects();
  };

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
              {globalUnreadCount > 0 && sidebarOpen && (
                  <span className={styles.unreadBadge}>{globalUnreadCount}</span>
              )}
            </li>
            <li className={styles.sidebarMenuItem} onClick={() => navigate('/Settings')}>
              <Settings size={22} style={{ marginRight: sidebarOpen ? 12 : 0 }} />
              {sidebarOpen && 'Settings'}
            </li>
          </ul>
        </aside>
        <main className={styles.mainContent}>
          <div className={styles.header}>
            <div className={styles.titleBox}>
              <span className={styles.title}>프로젝트</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
              <img src={defaultProfile} alt="프로필" style={{ width: 40, height: 40, borderRadius: '50%' }} />
              <button onClick={logout} className={styles.logoutBtn} style={{ marginLeft: 8 }}>로그아웃</button>
            </div>
          </div>
          <div className={styles.splitLayout}>
            <div className={styles.projectListSection} style={{ minWidth: 350, maxWidth: 600, flex: 2 }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', background: 'white', borderRadius: 8, overflow: 'hidden', boxShadow: '0 1px 4px rgba(0,0,0,0.04)' }}>
                <thead>
                <tr style={{ background: '#f5f7fa', color: '#333', fontWeight: 600, fontSize: 15 }}>
                  <th style={{ padding: '12px 8px', textAlign: 'left' }}>이름</th>
                  <th style={{ padding: '12px 8px', textAlign: 'left' }}>프로젝트 내용</th>
                  <th style={{ padding: '12px 8px', textAlign: 'left' }}>리더</th>
                  <th style={{ padding: '12px 8px', textAlign: 'left' }}>액션</th>
                </tr>
                </thead>
                <tbody>
                {projects.map((p, i) => (
                    <tr key={p.projectId || i} style={{ cursor: 'pointer', borderBottom: '1px solid #f0f0f0' }} onClick={() => navigate(`/task?project=${encodeURIComponent(p.projectTitle)}&projectId=${p.projectId}`)}>
                      <td style={{ padding: '10px 8px', fontWeight: 500 }}>
                        <span className={styles.projectDot} style={{ backgroundColor: p.color || '#007aff', marginRight: 8 }}></span>
                        {p.projectTitle}
                      </td>
                      <td style={{ padding: '10px 8px' }}>{p.description || '-'}</td>
                      <td style={{ padding: '10px 8px' }}>{p.owner?.username || '관리자'}</td>
                      <td style={{ padding: '10px 8px' }}>
                        <button onClick={(e) => { e.stopPropagation(); openEditModal(p); }} style={{ background: 'none', border: 'none' }}>
                          <Pencil size={18} color="#2563eb" />
                        </button>
                        <button onClick={(e) => { e.stopPropagation(); handleDeleteProject(p.projectId); }} style={{ background: 'none', border: 'none' }}>
                          <Trash2 size={18} color="#dc2626" />
                        </button>
                      </td>
                    </tr>
                ))}
                </tbody>
              </table>
              <div className={styles.addProjectText} onClick={() => { setShowProjectModal(true); setEditMode(false); setNewProject({ projectTitle: '', description: '', date: '' }); }}>
                + 프로젝트 추가
              </div>
            </div>
            <div className={styles.rightSection}>
              <div className={styles.notificationBox}>
                <h3 className={styles.notificationTitle}>알림</h3>
                <ul className={styles.notificationList}>
                  {notifications.map(n => <li key={n.id} className={styles.notificationItem}>{n.text}</li>)}
                </ul>
              </div>
              <div className={styles.calendarBox}>
                <Calendar onChange={setCalendarValue} value={calendarValue} className={styles.reactCalendar} />
              </div>
            </div>
          </div>
        </main>
        {showProjectModal && (
            <div className={styles.modalOverlay}>
              <div className={styles.modal}>
                <h3 className={styles.modalTitle}>{editMode ? '프로젝트 수정' : '프로젝트 추가'}</h3>
                <div className={styles.formContainer}>
                  <input type="text" placeholder="프로젝트 명" value={newProject.projectTitle} onChange={e => setNewProject({ ...newProject, projectTitle: e.target.value })} className={styles.formInput} />
                  <textarea placeholder="프로젝트 내용" value={newProject.description} onChange={e => setNewProject({ ...newProject, description: e.target.value })} className={styles.formTextarea} />
                  <input type="date" value={newProject.date} onChange={e => setNewProject({ ...newProject, date: e.target.value })} className={styles.formInput} />
                </div>
                <div className={styles.modalButtons}>
                  <button className={styles.btnSecondary} onClick={() => { setShowProjectModal(false); setEditMode(false); setEditProjectId(null); }}>취소</button>
                  <button className={styles.btnPrimary} onClick={handleAddOrEditProject}>{editMode ? '수정 완료' : '프로젝트 추가'}</button>
                </div>
              </div>
            </div>
        )}
      </div>
  );
}

export default Main;
