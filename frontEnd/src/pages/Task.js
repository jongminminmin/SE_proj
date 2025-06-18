import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { User, Calendar, Flag, MoreHorizontal, ChevronRight, ChevronLeft, Search, Folder, MessageCircle, Settings } from 'lucide-react'; // Plus 아이콘 제거
import styles from './Main.module.css';
import defaultProfile from '../assets/default-profile.png';

const columnOrder = ['todo', 'progress', 'review', 'done'];
const columnTitles = {
  todo: '해야 할 일',
  progress: '진행 중',
  review: '검토 중',
  done: '완료',
};

const Task = () => {
  const [tasks, setTasks] = useState([]);
  const [newTaskModal, setNewTaskModal] = useState(false);
  const [selectedColumn, setSelectedColumn] = useState('');
  const [taskForm, setTaskForm] = useState({
    projectId: '',
    taskTitle: '',
    assignee: '',
    description: '',
    dueStart: '',
    dueEnd: '',
    taskContent: '',
    status: 'todo',
  });
  const [projectIdFilter] = useState(''); // setProjectIdFilter는 사용되지 않으므로 제거

  const navigate = useNavigate();
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const projectTitle = queryParams.get('project');
  const currentProjectId = queryParams.get('projectId')

  const [showAssigneePicker, setShowAssigneePicker] =useState('');
  const [projectMembers, setProjectMembers] = useState([]);
  const [connectedUsers, setConnectedUsers] = useState(new Set());
  const [assigneeSearchTerm, setAssigneeSearchTerm] = useState(''); // 기본값 false에서 빈 문자열로 변경
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const filteredMembers = projectMembers.filter(member =>
      member.username.toLowerCase().includes(assigneeSearchTerm.toLowerCase())
  );


  const handleLogout = async () => {
    try { localStorage.removeItem('token'); } catch (e) {}
    try { await fetch('/logout', { method: 'POST' }); } catch (e) {}
    finally { navigate('/login'); }
  };

  // 1. 전체 Task 불러오기
  useEffect(() => {
    fetch('/api/tasks/')
        .then(res => res.json())
        .then(data => setTasks(data));
  }, []);

  // 2. Task 추가
  const handleAddTask = async () => {
    if (!taskForm.taskTitle.trim() || !taskForm.assignee || !taskForm.dueEnd) {
      alert('제목, 담당자, 마감일은 필수 입력 항목입니다.');
      return;
    }
    const taskToSend = { ...taskForm, status: taskForm.status || 'todo' };
    const res = await fetch('/api/tasks', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(taskToSend)
    });
    let saved = await res.json();
    if (!saved.status) saved.status = 'todo';
    setTasks(prev => [...prev, saved]);
    setNewTaskModal(false);
  };

  // 3. Task 수정
  const handleUpdateTask = async (updatedTask) => {
    await fetch('/api/tasks', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(updatedTask)
    });
    // 상태 변경 후 전체 목록 새로 불러오기
    const res = await fetch('/api/tasks');
    const data = await res.json();
    setTasks(data);
  };

  // 4. Task 삭제
  const handleDeleteTask = async (taskNo) => {
    await fetch('/api/tasks', {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(taskNo)
    });
    setTasks(prev => prev.filter(t => t.taskNo !== taskNo));
  };

  // 컬럼별로 tasks 분류
  const getColumnTasks = (status) =>
      tasks.filter(t => (projectIdFilter ? t.projectId === projectIdFilter : true) && (t.status === status));

  const priorityText = {
    high: '높음',
    medium: '보통',
    low: '낮음'
  };

  //현재 프로젝트 ID에 따라 프로젝트 멤버 불러오기
  useEffect(() => {
    if(currentProjectId) {
      //API : GET /api/projects/{projectId}/users 호출
      fetch(`/api/projects/${currentProjectId}/users`)
          .then(res => {
            if(!res.ok){
              throw new Error(`Failed to fetch project members for project ${currentProjectId}: ${res.statusText}`);
            }

            return res.json();
          })
          .then(data => {
            setProjectMembers(data);
            console.log("Fetched project members: ", data);
          })
          .catch(error => {
            console.error("Error fetching project members: ", error);
          });
    }
  }, [currentProjectId]);

  //온라인 사용자 목록을 주기적으로 가지고 옴
  //간소화를 위해 초기 렌더링 시 한 번 호출하고, 필요하면 주기적으로 호출하게 설정 가능
  useEffect(() => {
    const fetchConnectedUsers = async () => {
      try {
        //API : get /api/users/connected call
        const response = await fetch('/api/users/connected');
        if(response.ok){
          const userName = await response.json();
          setConnectedUsers(new Set(userName));
          console.log("Fetched Connected users:", userName);
        }
      } catch (error){
        console.error("Error fetching connected users:", error);
      }
    };

    fetchConnectedUsers();
    //주기적 업데이트
    const interval = setInterval(fetchConnectedUsers, 10000);
    return () => clearInterval(interval);
  },[]);

  // TaskCard 컴포넌트를 Task 컴포넌트 내부에서 정의하여 prop으로 전달하지 않아도 됨
  const TaskCard = useCallback(({ task }) => { // useCallback으로 감싸서 최적화
    const currentIndex = columnOrder.indexOf(task.status);
    const canMoveNext = currentIndex < columnOrder.length - 1;
    const canMovePrev = currentIndex > 0;
    return (
        <div className="task-card">
          <div className="task-header">
            <h4 className="task-title">{task.taskTitle}</h4>
            <div className="task-controls">
              {canMovePrev && (
                  <button onClick={() => handleUpdateTask({ ...task, status: columnOrder[currentIndex - 1] })} className="move-button prev-button" title="이전 단계로">
                    <ChevronLeft size={14} />
                  </button>
              )}
              {canMoveNext && (
                  <button onClick={() => handleUpdateTask({ ...task, status: columnOrder[currentIndex + 1] })} className="move-button next-button" title="다음 단계로">
                    <ChevronRight size={14} />
                  </button>
              )}
              <button onClick={() => handleDeleteTask(task.taskNo)} className="delete-task-btn">삭제</button>
              <MoreHorizontal size={16} className="more-menu" />
            </div>
          </div>
          <p className="task-description">{task.description}</p>
          <div className="task-labels">
            {task.taskContent && <span className="label">{task.taskContent}</span>}
          </div>
          <div className="task-footer">
            <div className="task-info">
              <div className="info-group">
                <User size={12} />
                <span>{task.assignee?.username || task.assignee}</span>
              </div>
              <div className="info-group">
                <Calendar size={12} />
                {/* task.assignee 대신 task.dueEnd를 표시하는 것이 적절할 수 있습니다. */}
                <span>{task.dueEnd}</span>
              </div>
            </div>
            <div className="priority">
              <Flag size={12} className={`priority-flag priority-${task.priority || 'medium'}`} />
              <span className={`priority-badge priority-${task.priority || 'medium'}`}>{priorityText[task.priority || 'medium']}</span>
            </div>
          </div>
        </div>
    );
  }, [handleUpdateTask, handleDeleteTask, priorityText]); // 의존성 추가: handleUpdateTask, handleDeleteTask, priorityText


  useEffect(() => {
    const columns = document.querySelectorAll('.kanban-hover');
    columns.forEach(col => {
      const btn = col.querySelector('.kanban-add-btn');
      if (btn) btn.style.display = 'inline-block';
    });
    return () => {
      columns.forEach(col => {
        const btn = col.querySelector('.kanban-add-btn');
        if (btn) btn.style.display = 'none';
      });
    };
  }, []);

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
            <li className={styles.sidebarMenuItem} onClick={() => navigate('/Settings')}> {/* Setting 페이지로 이동 */}
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
              <span className={styles.title}>{projectTitle || '업무'}</span>
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
          {/* 프로젝트 현황 표 */}
          <table style={{ width: '100%', background: '#fff', borderRadius: 12, boxShadow: '0 1px 3px rgba(0,0,0,0.08)', marginBottom: 24, borderCollapse: 'separate', borderSpacing: 0 }}>
            <thead>
            <tr style={{ background: '#f5f7fa', fontWeight: 600, fontSize: 15 }}>
              <th style={{ padding: '16px 0', textAlign: 'center' }}>전체 작업</th>
              <th style={{ padding: '16px 0', textAlign: 'center' }}>진행 중</th>
              <th style={{ padding: '16px 0', textAlign: 'center' }}>완료</th>
              <th style={{ padding: '16px 0', textAlign: 'center' }}>진행률</th>
            </tr>
            </thead>
            <tbody>
            <tr style={{ fontWeight: 500, fontSize: 18 }}>
              <td style={{ padding: '16px 0', textAlign: 'center', color: '#2563eb' }}>{tasks.length}</td>
              <td style={{ padding: '16px 0', textAlign: 'center', color: '#d97706' }}>{getColumnTasks('progress').length}</td>
              <td style={{ padding: '16px 0', textAlign: 'center', color: '#059669' }}>{getColumnTasks('done').length}</td>
              <td style={{ padding: '16px 0', textAlign: 'center', color: '#7c3aed' }}>{tasks.length > 0 ? Math.round((getColumnTasks('done').length / tasks.length) * 100) + '%' : '0%'}</td>
            </tr>
            </tbody>
          </table>
          {/* 칸반 보드 */}
          <div style={{ display: 'flex', gap: 12, height: 'calc(100vh - 220px)' }}>
            {columnOrder.map((col) => (
                <div
                    key={col}
                    style={{
                      flex: 1,
                      background: '#fafbfc',
                      borderRadius: 12,
                      padding: 16,
                      minWidth: 220,
                      display: 'flex',
                      flexDirection: 'column',
                      position: 'relative',
                      minHeight: 120,
                      transition: 'min-height 0.2s, height 0.2s',
                      height: getColumnTasks(col).length > 0 ? undefined : 120,
                    }}
                    onMouseEnter={e => e.currentTarget.classList.add('kanban-hover')}
                    onMouseLeave={e => e.currentTarget.classList.remove('kanban-hover')}
                >
                  {/* 컬럼 헤더 */}
                  <div style={{ fontWeight: 600, fontSize: 17, marginBottom: 12, display: 'flex', alignItems: 'center', gap: 6, position: 'relative' }}>
                    {columnTitles[col]}
                    {col === 'done' && <span style={{ color: '#6bb700', fontSize: 18, marginLeft: 2 }}>✔</span>}
                    <span style={{ color: '#888', fontWeight: 400, fontSize: 14, marginLeft: 6 }}>{getColumnTasks(col).length > 0 ? getColumnTasks(col).length : ''}</span>
                  </div>
                  {/* 카드 리스트 */}
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                    {getColumnTasks(col).map((task) => (
                        <TaskCard key={task.taskNo} task={task} /> // TaskCard 컴포넌트 사용
                    ))}
                  </div>
                  {/* 하단 중앙 +만들기 문구 */}
                  <div style={{ display: 'flex', justifyContent: 'center', marginTop: 16, marginBottom: 4 }}>
                <span
                    className={styles.kanbanAddText}
                    onClick={() => {
                      setSelectedColumn(col);
                      setTaskForm({ ...taskForm, status: col });
                      setNewTaskModal(true);
                    }}
                    style={{
                      color: '#2563eb',
                      fontWeight: 500,
                      fontSize: 16,
                      cursor: 'pointer',
                      padding: '6px 18px',
                      borderRadius: 6,
                      background: 'transparent',
                      transition: 'background 0.15s',
                    }}
                    onMouseOver={e => e.currentTarget.style.background = '#f0f4ff'}
                    onMouseOut={e => e.currentTarget.style.background = 'transparent'}
                >
                  <span style={{ fontSize: 20, marginRight: 4, fontWeight: 700 }}>+</span>만들기
                </span>
                  </div>
                </div>
            ))}
          </div>
          {/* 새 작업 추가 모달 */}
          {newTaskModal && (
              <div className="modal-overlay">
                <div className="modal">
                  <h3 className="modal-title">새 작업 추가 - {columnTitles[selectedColumn]}</h3>
                  <div className="form-container">
                    <input
                        type="text"
                        placeholder="작업 제목 *"
                        value={taskForm.taskTitle}
                        onChange={e => setTaskForm({ ...taskForm, taskTitle: e.target.value })}
                        className="form-input"
                    />
                    <textarea
                        placeholder="작업 설명"
                        value={taskForm.description}
                        onChange={e => setTaskForm({ ...taskForm, description: e.target.value })}
                        className="form-textarea"
                    />

                    {/* 담당자 선택기 (슬라이드 뷰 대체) */}
                    <div style={{ position: 'relative', width: '100%' }}>
                      <div
                          className="form-input" // 기존 input 스타일 재활용
                          style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer' }}
                          onClick={() => setShowAssigneePicker(true)}
                      >
                        <span>담당자: {taskForm.assignee ? taskForm.assignee : '선택하세요 *'}</span>
                        <User size={16} />
                      </div>

                      {showAssigneePicker && (
                          <div style={{
                            position: 'absolute',
                            top: '100%',
                            left: 0,
                            right: 0,
                            backgroundColor: 'white',
                            border: '1px solid #E5E7EB',
                            borderRadius: '8px',
                            zIndex: 1000,
                            boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
                            maxHeight: '300px',
                            overflowY: 'auto',
                            marginTop: '4px',
                            padding: '8px'
                          }}>
                            <div style={{ display: 'flex', alignItems: 'center', marginBottom: '8px', borderBottom: '1px solid #eee', paddingBottom: '8px' }}>
                              <Search size={18} style={{ marginRight: '8px', color: '#6B7280' }} />
                              <input
                                  type="text"
                                  placeholder="담당자 검색..."
                                  value={assigneeSearchTerm}
                                  onChange={(e) => setAssigneeSearchTerm(e.target.value)}
                                  style={{ flex: 1, border: 'none', outline: 'none', fontSize: '14px' }}
                              />
                              <button onClick={() => setShowAssigneePicker(false)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#6B7280' }}>X</button>
                            </div>
                            <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                              {projectMembers
                                  .filter(member => member.username.toLowerCase().includes(assigneeSearchTerm.toLowerCase()))
                                  .map(member => (
                                      <li
                                          key={member.id}
                                          style={{
                                            padding: '8px 12px',
                                            cursor: 'pointer',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'space-between',
                                            backgroundColor: taskForm.assignee === member.username ? '#E5E7EB' : 'transparent',
                                            borderRadius: '4px'
                                          }}
                                          onClick={() => {
                                            setTaskForm({ ...taskForm, assignee: member.username }); // 담당자로 username 설정
                                            setShowAssigneePicker(false); // 선택 후 닫기
                                            setAssigneeSearchTerm(''); // 검색어 초기화
                                          }}
                                      >
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                          <User size={16} color="#6B7280" /> {/* 실제 아바타 이미지로 교체 가능 */}
                                          <span>{member.username}</span>
                                        </div>
                                        {/* 온라인 상태 표시 */}
                                        <div style={{
                                          width: '10px',
                                          height: '10px',
                                          borderRadius: '50%',
                                          backgroundColor: connectedUsers.has(member.username) ? '#10B981' : '#6B7280' // 온라인: 초록, 오프라인: 회색
                                        }}></div>
                                      </li>
                                  ))}
                              {projectMembers.length === 0 && !assigneeSearchTerm && (
                                  <li style={{ padding: '8px', textAlign: 'center', color: '#6B7280' }}>
                                    프로젝트 멤버가 없습니다.
                                  </li>
                              )}
                              {projectMembers.length > 0 && filteredMembers.length === 0 && assigneeSearchTerm && (
                                  <li style={{ padding: '8px', textAlign: 'center', color: '#6B7280' }}>
                                    "{assigneeSearchTerm}"에 해당하는 사용자가 없습니다.
                                  </li>
                              )}
                            </ul>
                          </div>
                      )}
                    </div>
                    {/* 기존 담당자 입력 필드는 위 담당자 선택기로 대체됨 */}
                    {/* <input type="text" placeholder="담당자(이름 또는 ID) *" value={taskForm.assignee} onChange={e => setTaskForm({ ...taskForm, assignee: e.target.value })} className="form-input" /> */}

                    <input type="date" value={taskForm.dueEnd} onChange={e => setTaskForm({ ...taskForm, dueEnd: e.target.value })} className="form-input" />
                    <textarea placeholder="작업 내용" value={taskForm.taskContent} onChange={e => setTaskForm({ ...taskForm, taskContent: e.target.value })} className="form-textarea" />
                  </div>
                  <div className="modal-buttons">
                    <button onClick={() => setNewTaskModal(false)} className="btn btn-secondary">취소</button>
                    <button onClick={handleAddTask} className="btn btn-primary">추가</button>
                  </div>
                </div>
              </div>
          )}
        </main>
      </div>
  );
};

export default Task;
