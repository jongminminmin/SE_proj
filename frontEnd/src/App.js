import React, {createContext, useContext, useEffect, useState} from 'react';
import {Navigate, Outlet, Route, Routes, useNavigate} from 'react-router-dom';
import './App.css';
import Login from './pages/Login'; // Login 컴포넌트에서 useAuth().login() 호출 필요
import FindAccount from './pages/FindAccount';
import Register from './pages/Register';
import Main from './pages/Main';
import Task from './pages/Task';
import Chat from './pages/Chat';

// 1. 인증 컨텍스트 생성
const AuthContext = createContext(null);

// 2. AuthProvider 컴포넌트 정의
const AuthProvider = ({ children }) => {
  const [currentUser, setCurrentUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true); // 인증 상태 로딩 여부
  const navigate = useNavigate(); // App 컴포넌트가 BrowserRouter 내에 있으므로 여기서 useNavigate 사용 가능

  useEffect(() => {
    const checkAuthStatus = async () => {
      setIsLoading(true);
      try {
        const response = await fetch('/api/users/me', { credentials: 'include' });

        if (response.redirected && response.url.includes("/login")) { // 로그인 페이지로 리디렉션 되었는지 확인
          console.log("User not authenticated, redirected to login page by server.");
          setCurrentUser(null);
        } else if (response.ok) {
          const contentType = response.headers.get("content-type");
          if (contentType && contentType.indexOf("application/json") !== -1) {
            const userData = await response.json();
            setCurrentUser({ /* ... userData ... */ });
          } else { // 200 OK지만 JSON이 아닌 경우 (예: SPA fallback으로 index.html이 온 경우)
            console.warn("checkAuthStatus received OK but non-JSON. Assuming unauthenticated.");
            setCurrentUser(null);
          }
        } else { // 401, 403 등 명시적 에러 또는 기타 ok:false
          console.warn(`checkAuthStatus failed (status: ${response.status}). Assuming unauthenticated.`);
          setCurrentUser(null);
        }
      } catch (error) { // 네트워크 에러 등
        console.error("Error during checkAuthStatus (network/fetch error):", error);
        setCurrentUser(null);
      } finally {
        setIsLoading(false);
      }
    };
    checkAuthStatus();
  }, []);

  // 로그인 성공 시 호출될 함수 (Login 컴포넌트에서 이 함수를 호출)
  const login = (userData) => {
    setCurrentUser(userData);
    // 필요시 추가 로직 (예: 사용자 정보 전역 상태 저장)
  };

  // 로그아웃 함수
  const logout = async () => {
    try {
      // Spring Security의 기본 로그아웃은 보통 GET /logout 이지만,
      // CSRF가 활성화되어 있다면 POST /logout을 사용하고 CSRF 토큰을 보내야 합니다.
      // 현재 SecurityConfig에서 CSRF가 disable 되어있으므로 GET/POST 모두 가능할 수 있습니다.
      // Spring Security는 로그아웃 성공 시 세션 쿠키를 무효화합니다.
      const response = await fetch('/logout', { // 백엔드의 로그아웃 URL
        method: 'POST', // CSRF disable 시 GET도 가능하나 POST가 더 적절할 수 있음
        credentials: 'include',
        // CSRF가 활성화된 상태라면 CSRF 토큰 헤더를 추가해야 합니다.
      });
      if (response.ok || response.redirected) { // 로그아웃 성공 또는 리다이렉션 발생 시
        setCurrentUser(null);
        navigate('/login'); // 로그인 페이지로 이동
      } else {
        console.error("Logout failed:", response.status);
        // 사용자에게 로그아웃 실패 알림 가능
      }
    } catch (error) {
      console.error("Error during logout:", error);
    }
  };

  return (
      <AuthContext.Provider value={{ currentUser, setCurrentUser, login, logout, isLoading }}>
        {children}
      </AuthContext.Provider>
  );
};

// 3. useAuth 커스텀 훅 정의
export const useAuth = () => {
  return useContext(AuthContext);
};

// 4. ProtectedRoute 컴포넌트 정의
const ProtectedRoute = () => {
  const { currentUser, isLoading } = useAuth();

  if (isLoading) {
    // 인증 상태 확인 중일 때 로딩 인디케이터 등을 보여줄 수 있습니다.
    return <div>애플리케이션 로딩 중...</div>;
  }

  // currentUser가 있으면 요청된 자식 라우트(Outlet)를 렌더링하고,
  // 없으면 로그인 페이지로 리디렉션합니다.
  return currentUser ? <Outlet /> : <Navigate to="/login" replace />;
};

function App() {
  return (
      // BrowserRouter는 index.js나 App.js 최상단에 한 번만 사용하는 것이 일반적입니다.
      // 만약 index.js에 BrowserRouter가 이미 있다면 여기서는 제거해야 합니다.
      // 여기서는 App.js에 BrowserRouter가 있다고 가정합니다.
        <AuthProvider> {/* 모든 라우트를 AuthProvider로 감싸서 인증 상태 공유 */}
          <div className="App">
            {/* 로그아웃 버튼 예시 (네비게이션 바 등에 위치시킬 수 있음) */}
            {/* <AuthButton /> */}
            <Routes>
              <Route path="/" element={<Login />} />
              <Route path="/login" element={<Login />} />
              <Route path="/find-account" element={<FindAccount />} />
              <Route path="/register" element={<Register />} />

              {/* 보호된 라우트 설정 */}
              <Route element={<ProtectedRoute />}>
                <Route path="/main" element={<Main />} />
                <Route path="/task" element={<Task />} />
                <Route path="/chat" element={<Chat />} />
                {/* 추가적으로 로그인이 필요한 모든 경로는 여기에 배치 */}
              </Route>

              {/* 매칭되는 라우트가 없을 때 보여줄 페이지 (선택 사항) */}
              {/* <Route path="*" element={<div>페이지를 찾을 수 없습니다.</div>} /> */}
            </Routes>
          </div>
        </AuthProvider>
  );
}

// 로그아웃 버튼 컴포넌트 예시 (별도 파일로 분리 가능)
// const AuthButton = () => {
//   const { currentUser, logout } = useAuth();
//   const navigate = useNavigate();

//   if (!currentUser) {
//     return <button onClick={() => navigate('/login')}>로그인</button>;
//   }

//   return (
//     <div>
//       <span>안녕하세요, {currentUser.name || currentUser.id}님!</span>
//       <button onClick={logout}>로그아웃</button>
//     </div>
//   );
// };

export default App;