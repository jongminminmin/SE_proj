import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './Main.module.css';

const taskList = [
  {
    title: 'UI 디자인 검토',
    desc: '오늘 오후 3시까지',
    status: 'progress',
    color: '',
  },
  {
    title: 'API 문서화',
    desc: '내일까지',
    status: 'progress',
    color: '#f0fff0',
  },
  {
    title: '서버 배포',
    desc: '지난 주 완료',
    status: 'completed',
    color: '',
  },
];

const tabList = [
  { key: 'all', label: '전체' },
  { key: 'progress', label: '진행중' },
  { key: 'completed', label: '완료' },
];

function Task() {
  const [tab, setTab] = useState('all');
  const [projectOpen, setProjectOpen] = useState(true);
  const navigate = useNavigate();

  const filteredTasks =
    tab === 'all'
      ? taskList
      : taskList.filter((t) => t.status === tab);

  return (
    <div className={styles.wrapper}>
      <aside className={styles.sidebar}>
        <div className={styles.sidebarTitle}>MATE</div>
        <ul className={styles.sidebarMenu}>
          <li className={styles.sidebarMenuItem} onClick={() => { setProjectOpen((v) => !v); navigate('/main'); }}>
            <span role="img" aria-label="project" className={styles.icon}>📋</span> Project
          </li>
          {projectOpen && (
            <ul className={styles.sidebarSubMenu}>
              <li className={`${styles.sidebarSubMenuItem} ${styles.active}`}>
                <span role="img" aria-label="task" className={styles.iconSub} onClick={() => navigate('/task')}>📝</span> Task
              </li>
            </ul>
          )}
          <li className={styles.sidebarMenuItem} onClick={() => alert('채팅 페이지로 이동합니다.')}>
            <span role="img" aria-label="chat" className={styles.icon}>💬</span> Chat
          </li>
          <li className={styles.sidebarMenuItem}>
            <span role="img" aria-label="settings" className={styles.icon}>⚙️</span> Settings
          </li>
        </ul>
      </aside>
      <main className={styles.mainContent}>
        <div className={styles.header}>
          <div className={styles.titleBox}>
            <h1 className={styles.title}>Task</h1>
          </div>
          <button className={styles.logoutBtn}>로그아웃</button>
        </div>
        <div className={styles.tabs}>
          {tabList.map((t) => (
            <button
              key={t.key}
              className={tab === t.key ? `${styles.tab} ${styles.activeTab}` : styles.tab}
              onClick={() => setTab(t.key)}
            >
              {t.label}
            </button>
          ))}
        </div>
        <div className={styles.section}>
          <div className={styles.sectionHeader}>
            <div>
              <h2 className={styles.sectionTitle}>Task</h2>
              <div className={styles.sectionSubtitle}>할 일을 관리하세요</div>
            </div>
          </div>
          <div className={styles.cardGrid}>
            <div className={`${styles.card} ${styles.addCard}`} onClick={() => alert('새 태스크 추가')}>
              <div className={styles.addCardIcon}>+</div>
              <div className={styles.addCardText}>새 태스크 추가</div>
            </div>
            {filteredTasks.map((t, i) => (
              <div className={styles.card} key={i}>
                <div
                  className={styles.cardContentPlaceholder}
                  style={{ backgroundColor: t.color || '#f0f0f0' }}
                ></div>
                <div className={styles.cardInfo}>
                  <div className={styles.cardTitle}>{t.title}</div>
                  <div className={styles.cardDescription}>{t.desc}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </main>
    </div>
  );
}

export default Task; 