import React, { useState, useEffect, useCallback, useRef, memo, createContext, useContext } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { User, Calendar, MoreHorizontal, Folder, MessageCircle, Settings, UserPlus, Plus } from 'lucide-react';
import styles from './Task.module.css';
import './Task.css';
import defaultProfile from '../assets/default-profile.png';
import TaskDetailModal from './TaskDetailModal';
import { useAuth } from '../App';

const columnOrder = ['todo', 'progress', 'review', 'done'];
const columnTitles = {
  todo: '해야 할 일',
  progress: '진행 중',
  review: '검토 중',
  done: '완료',
};

const TaskContext = createContext();

const MoreMenu = ({ task, onUpdateStatus, onDelete }) => {
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef(null);
  const { setAnyMenuOpen } = useContext(TaskContext);
  const availableStatuses = columnOrder.filter(status => status !== task.status);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    if (setAnyMenuOpen) {
      setAnyMenuOpen(isOpen);
    }
    return () => {
      if (setAnyMenuOpen) {
        setAnyMenuOpen(false);
      }
    };
  }, [isOpen, setAnyMenuOpen]);

  return (
    <div className="more-menu-container" ref={menuRef}>
      <button onClick={(e) => { e.stopPropagation(); setIsOpen(p => !p); }} className="more-menu-button">
        <MoreHorizontal size={16} />
      </button>
      {isOpen && (
        <div className="more-menu-dropdown">
          <div className="more-menu-header">상태 변경</div>
          {availableStatuses.map(status => (
            <div key={status} className="more-menu-item" onClick={(e) => { e.stopPropagation(); onUpdateStatus(status); setIsOpen(false); }}>
              <span>{columnTitles[status]}</span>
            </div>
          ))}
          <div className="more-menu-divider"></div>
          <div className="more-menu-item" onClick={(e) => { e.stopPropagation(); onDelete(); setIsOpen(false); }}>
            <span>삭제</span>
          </div>
        </div>
      )}
    </div>
  );
};

const TaskCard = memo(({ task, onClick }) => {
  const { handleUpdateTaskStatus, handleDeleteTask } = useContext(TaskContext);
  const assignee = task.assignee;

  return (
    <div className="task-card" onClick={() => onClick(task)}>
      <div className="task-card-header">
        <span className="task-card-title">{task.taskTitle}</span>
        <MoreMenu
          task={task}
          onUpdateStatus={(newStatus) => handleUpdateTaskStatus(task.taskNo, newStatus)}
          onDelete={() => handleDeleteTask(task.taskNo)}
        />
      </div>
      <p className="task-card-description">{task.description}</p>
      <div className="task-card-footer">
        <div className="task-card-assignee">
          {assignee ? (
            <>
              <img
                src={defaultProfile}
                alt={assignee.username || 'assignee'}
                className="assignee-profile-pic"
              />
              <span>{assignee.username}</span>
            </>
          ) : (
            <div className="assignee-placeholder">
              <User size={16} color="#888" />
              <span style={{ marginLeft: '4px', color: '#888' }}>미지정</span>
            </div>
          )}
        </div>
        <span className="task-card-due-date">
          {task.dueEnd ? new Date(task.dueEnd).toLocaleDateString() : '기한 없음'}
        </span>
      </div>
    </div>
  );
});

const Task = () => {
  const [tasks, setTasks] = useState([]);
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [inviteIdentifier, setInviteIdentifier] = useState('');
  const [showTaskModal, setShowTaskModal] = useState(false);
  const [anyMenuOpen, setAnyMenuOpen] = useState(false);
  const [selectedTask, setSelectedTask] = useState(null);
  const [projectMembers, setProjectMembers] = useState([]);
  const { currentUser } = useAuth();
  const [newTaskData, setNewTaskData] = useState({
    taskTitle: '',
    description: '',
    assigneeId: '',
    dueEnd: '',
    status: ''
  });

  const navigate = useNavigate();
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const projectTitle = queryParams.get('project');
  const currentProjectId = queryParams.get('projectId');

  const fetchTasks = useCallback(async () => {
    if (!currentProjectId) return;
    try {
      const res = await fetch(`/api/tasks/project/${currentProjectId}`);
      if (res.ok) {
        const data = await res.json();
        setTasks(data);
      } else {
        console.error("Failed to fetch tasks for project");
      }
    } catch (error) {
      console.error("Error fetching tasks:", error);
    }
  }, [currentProjectId]);

  useEffect(() => {
    const fetchProjectMembers = async () => {
      if (!currentProjectId) return;
      try {
        const res = await fetch(`/api/projects/${currentProjectId}/members`);
        if (res.ok) {
          const membersData = await res.json();
          setProjectMembers(membersData || []);
        } else {
          console.error("Failed to fetch project members");
          setProjectMembers([]);
        }
      } catch (error) {
        console.error("Error fetching project members:", error);
        setProjectMembers([]);
      }
    };
    fetchProjectMembers();
  }, [currentProjectId]);

  useEffect(() => {
    fetchTasks();
    const eventSource = new EventSource(`/api/sse/subscribe`);

    eventSource.addEventListener('tasks-updated', (event) => {
      const updatedData = JSON.parse(event.data);
      
      setTasks(currentTasks => {
        if (updatedData.deletedTaskId) {
          return currentTasks.filter(t => t.taskNo !== updatedData.deletedTaskId);
        }

        const taskExists = currentTasks.some(t => t.taskNo === updatedData.taskNo);
        if (taskExists) {
          return currentTasks.map(t => (t.taskNo === updatedData.taskNo ? updatedData : t));
        } else {
          return [...currentTasks, updatedData];
        }
      });
    });

    eventSource.onerror = (error) => {
      console.error('SSE error:', error);
      eventSource.close();
    };
    return () => eventSource.close();
  }, [fetchTasks]);

  const openCreateTaskModal = (status) => {
    setNewTaskData({
      taskTitle: '',
      description: '',
      status: status,
      assigneeId: null,
      dueEnd: ''
    });
    setShowTaskModal(true);
  };

  const handleCreateTask = async () => {
    if (!newTaskData.taskTitle) {
      alert('업무 제목을 입력해주세요.');
      return;
    }
    if (!currentProjectId) return;

    const taskPayload = {
      taskTitle: newTaskData.taskTitle,
      description: newTaskData.description,
      status: newTaskData.status,
      projectId: parseInt(currentProjectId, 10),
      assigneeId: newTaskData.assigneeId ? newTaskData.assigneeId : null,
      dueEnd: newTaskData.dueEnd,
    };

    const response = await fetch('/api/tasks', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(taskPayload),
    });

    if (response.ok) {
      setShowTaskModal(false);
    } else {
      alert('업무 생성에 실패했습니다.');
    }
  };

  const handleUpdateTaskStatus = async (taskNo, newStatus) => {
    const originalTasks = tasks;
    const newTasks = tasks.map(t =>
      t.taskNo === taskNo ? { ...t, status: newStatus } : t
    );
    setTasks(newTasks);

    const taskToUpdate = originalTasks.find(t => t.taskNo === taskNo);
    if (!taskToUpdate) return;
    
    try {
      const response = await fetch(`/api/tasks/${taskNo}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: newStatus }),
      });

      if (!response.ok) {
        setTasks(originalTasks);
        alert('업무 상태 변경에 실패했습니다.');
      }
    } catch (error) {
      console.error("Error updating task status:", error);
      setTasks(originalTasks);
      alert('업무 상태 변경 중 오류가 발생했습니다.');
    }
  };

  const handleDeleteTask = async (taskNo) => {
    const taskToDelete = tasks.find(t => t.taskNo === taskNo);
    if (window.confirm(`'${taskToDelete.taskTitle}' 작업을 삭제하시겠습니까?`)) {
      await fetch(`/api/tasks/${taskNo}`, { method: 'DELETE' });
    }
  };

  const handleInvite = async () => {
    if (!inviteIdentifier) {
      alert('초대할 사용자의 아이디를 입력해주세요.');
      return;
    }
    try {
      const response = await fetch(`/api/projects/${currentProjectId}/invite`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: inviteIdentifier }),
      });
      if (response.ok) {
        alert('초대가 성공적으로 전송되었습니다.');
        setShowInviteModal(false);
        setInviteIdentifier('');
      } else {
        const errorData = await response.json();
        alert(`초대 실패: ${errorData.message}`);
      }
    } catch (error) {
      console.error('Invite error:', error);
      alert('초대 중 오류가 발생했습니다.');
    }
  };

  const getColumnTasks = (status) => tasks.filter(t => t.status === status);

  const handleTaskClick = (task) => {
    setSelectedTask(task);
  };

  const handleCloseDetailModal = () => {
    setSelectedTask(null);
  };

  const handleSelectAssignee = (memberId) => {
    setNewTaskData({ ...newTaskData, assigneeId: memberId });
  };

  const getAssigneeName = (assigneeId) => {
    const member = projectMembers.find(m => m.id === assigneeId);
    return member ? member.username : '미지정';
  };

  const getAssigneeProfile = (assigneeId) => {
    // 항상 기본 프로필 반환
    return defaultProfile;
  };

  return (
    <TaskContext.Provider value={{ handleUpdateTaskStatus, handleDeleteTask, setAnyMenuOpen }}>
      <div className={styles.taskPageContainer}>
        <header className={styles.header}>
          <div className={styles.titleBox}>
            <span className={styles.title}>{projectTitle || '업무'}</span>
            <button onClick={() => setShowInviteModal(true)} className={styles.inviteBtn}>
              <UserPlus size={16} />
              <span>팀원 초대</span>
            </button>
          </div>
        </header>

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

        <div className="task-board">
          {columnOrder.map(status => (
            <div key={status} className={`task-column ${anyMenuOpen ? 'menu-open' : ''}`}>
              <div className="column-header">
                <span className="column-title">{columnTitles[status]}</span>
                <span className="task-count">{getColumnTasks(status).length}</span>
              </div>
              <div className="task-list">
                {getColumnTasks(status).map(task => (
                  <TaskCard key={task.taskNo} task={task} onClick={handleTaskClick} />
                ))}
              </div>
              <button className="create-task-btn" onClick={() => openCreateTaskModal(status)}>
                <Plus size={16} style={{ marginRight: '8px' }} />
                <span>만들기</span>
              </button>
            </div>
          ))}
        </div>
      </div>

      {selectedTask && (
        <TaskDetailModal
          task={selectedTask}
          onClose={handleCloseDetailModal}
          onUpdateTask={fetchTasks}
          currentUser={currentUser}
        />
      )}

      {showTaskModal && (
        <div className="modal-overlay">
          <div className="modal">
            <h3 className="modal-title">새 업무 만들기</h3>
            <div className="form-container" style={{gap: '12px'}}>
              <input
                type="text"
                placeholder="업무 제목"
                value={newTaskData.taskTitle}
                onChange={(e) => setNewTaskData({ ...newTaskData, taskTitle: e.target.value })}
                className="form-input"
              />
              <textarea
                placeholder="업무 설명"
                value={newTaskData.description}
                onChange={(e) => setNewTaskData({ ...newTaskData, description: e.target.value })}
                className="form-input"
                rows="3"
              />
              <select
                value={newTaskData.assigneeId}
                onChange={(e) => setNewTaskData({ ...newTaskData, assigneeId: e.target.value })}
                className="form-input"
              >
                <option value="">담당자 선택</option>
                {projectMembers.map(member => (
                  <option key={member.id} value={member.id}>{member.username}</option>
                ))}
              </select>
              <input
                type="date"
                value={newTaskData.dueEnd}
                onChange={(e) => setNewTaskData({ ...newTaskData, dueEnd: e.target.value })}
                className="form-input"
              />
            </div>
            <div className="modal-buttons">
              <button onClick={() => setShowTaskModal(false)} className="btn btn-secondary">취소</button>
              <button onClick={handleCreateTask} className="btn btn-primary">만들기</button>
            </div>
          </div>
        </div>
      )}

      {showInviteModal && (
        <div className="modal-overlay">
          <div className="modal">
            <h3 className="modal-title">팀원 초대</h3>
            <div className="form-container">
              <input
                type="text"
                placeholder="초대할 사용자의 아이디를 입력하세요"
                value={inviteIdentifier}
                onChange={(e) => setInviteIdentifier(e.target.value)}
                className="form-input"
              />
            </div>
            <div className="modal-buttons">
              <button onClick={() => setShowInviteModal(false)} className="btn btn-secondary">취소</button>
              <button onClick={handleInvite} className="btn btn-primary">초대</button>
            </div>
          </div>
        </div>
      )}
    </TaskContext.Provider>
  );
};

export default Task;
