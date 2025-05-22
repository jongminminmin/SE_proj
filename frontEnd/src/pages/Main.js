import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './Main.module.css';

const projectList = [
  {
    title: '프로젝트 알파',
    desc: '차세대 AI 기반 서비스 개발',
    status: 'progress',
    color: '',
  },
  {
    title: '프로젝트 베타',
    desc: '데이터 분석 플랫폼 구축',
    status: 'completed',
    color: '#e6f7ff',
  },
  {
    title: '프로젝트 감마',
    desc: '모바일 앱 리뉴얼',
    status: 'progress',
    color: '#fff7e6',
  },
];

const tabList = [
  { key: 'all', label: '전체' },
  { key: 'progress', label: '진행중' },
  { key: 'completed', label: '완료' },
];

function Main() {
  const [tab, setTab] = useState('all');
  const [projectOpen, setProjectOpen] = useState(true);
  const navigate = useNavigate();

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
          ? projectList
          : projectList.filter((p) => p.status === tab);

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
              <h1 className={styles.title}>대시보드</h1>
              {/* <p>환영합니다, 사용자님!</p> */}
            </div>
            {/* 로그아웃 버튼에 onClick 이벤트 핸들러 연결 */}
            <button className={styles.logoutBtn} onClick={handleLogout}>로그아웃</button>
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
                <h2 className={styles.sectionTitle}>Project</h2>
                <div className={styles.sectionSubtitle}>프로젝트를 관리하세요</div>
              </div>
            </div>
            {/* ESLint: 'filteredProjects' is used here. It should be defined in this scope. */}
            {/* Line 141 in your error message might refer to this block or the .map call specifically. */}
            <div className={styles.cardGrid}>
              <div className={`${styles.card} ${styles.addCard}`} onClick={() => alert('새 프로젝트 추가')}>
                <div className={styles.addCardIcon}>+</div>
                <div className={styles.addCardText}>새 프로젝트 추가</div>
              </div>
              {filteredProjects.map((p, i) => (
                  <div className={styles.card} key={i}>
                    <div
                        className={styles.cardContentPlaceholder}
                        style={{ backgroundColor: p.color || '#f0f0f0' }}
                    ></div>
                    <div className={styles.cardInfo}>
                      <div className={styles.cardTitle}>{p.title}</div>
                      <div className={styles.cardDescription}>{p.desc}</div>
                    </div>
                  </div>
              ))}
            </div>
          </div>
        </main>
      </div>
  );
}

export default Main;
