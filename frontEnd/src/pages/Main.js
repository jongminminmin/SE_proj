import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useAuth} from '../App';
import styles from './Main.module.css';
import Calendar from 'react-calendar';
import 'react-calendar/dist/Calendar.css';
import { useNotifications } from '../NotificationContext';

const Pencil = ({ size, color }) => <svg width={size} height={size} color={color} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path></svg>;
const Trash2 = ({ size, color }) => <svg width={size} height={size} color={color} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>;

function Main() {
  const [showProjectModal, setShowProjectModal] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [editProjectId, setEditProjectId] = useState(null);
  const [newProject, setNewProject] = useState({ projectTitle: '', description: '', date: '' });
  const [projects, setProjects] = useState([]);
  const [calendarValue, setCalendarValue] = useState(new Date());
  const navigate = useNavigate();
  const { currentUser } = useAuth();
  const { updateGlobalUnreadCount } = useNotifications();

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
      const response = await fetch(`/api/projects/${editProjectId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...dto, projectId: editProjectId })
      });
      if (!response.ok) {
        if (response.status === 403) {
            alert('오류: 프로젝트를 수정할 권한이 없습니다.');
        } else {
            const errorData = await response.json().catch(() => ({ message: '알 수 없는 오류가 발생했습니다.' }));
            alert(`오류: ${errorData.message}`);
        }
        return; // 실패 시 함수 종료
      }
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
    const response = await fetch(`/api/projects/${projectId}`, { method: 'DELETE' });
    if (!response.ok) {
        if (response.status === 403) {
            alert('오류: 프로젝트를 삭제할 권한이 없습니다.');
        } else {
            alert('프로젝트 삭제 중 오류가 발생했습니다.');
        }
        return;
    }
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
      <div className={styles.mainContainer}>
          <div className={styles.header}>
            <div className={styles.titleBox}>
              <span className={styles.title}>프로젝트</span>
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
