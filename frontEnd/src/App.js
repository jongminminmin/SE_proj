import React, {createContext, useContext, useEffect, useState} from 'react';
import {Navigate, Outlet, Route, Routes, useNavigate} from 'react-router-dom';
import './App.css';
import Login from './pages/Login'; // Login 컴포넌트에서 useAuth().login() 호출 필요
import FindAccount from './pages/FindAccount';
import Register from './pages/Register';
import Main from './pages/Main';
import Task from './pages/Task';
import Chat from './pages/Chat';
import Settings from './pages/Settings';

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
            setCurrentUser(userData);
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

    // checkAuthStatus 함수를 호출하고, 그 반환값(Promise)에 .catch()를 연결합니다.
    checkAuthStatus().catch(console.error);
  }, []); // 의존성 배열에 아무것도 없으므로 컴포넌트 마운트 시 한 번만 실행

  // 로그인 성공 시 호출될 함수 (Login 컴포넌트에서 이 함수를 호출)
  const login = (userData) => {
    setCurrentUser(userData);
    // 필요시 추가 로직 (예: 사용자 정보 전역 상태 저장)
  };

  // 로그아웃 함수
  const logout = async () => {
    try {
      const response = await fetch('/logout', {
        method: 'POST',
        credentials: 'include',
      });
      if (response.ok || response.redirected) {
        setCurrentUser(null);
        navigate('/login');
      } else {
        console.error("Logout failed:", response.status);
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
    return <div>애플리케이션 로딩 중...</div>;
  }

  return currentUser ? <Outlet /> : <Navigate to="/login" replace />;
};

function App() {
  return (
      <AuthProvider>
        <div className="App">
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
              <Route path="/Settings" element={<Settings />} />
            </Route>
          </Routes>
        </div>
      </AuthProvider>
  );
}

export default App;
