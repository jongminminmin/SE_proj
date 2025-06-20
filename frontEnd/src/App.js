import React, { createContext, useContext, useEffect, useState } from 'react';
import { Navigate, Outlet, Route, Routes, useNavigate } from 'react-router-dom';
import './App.css';
import Login from './pages/Login';
import FindAccount from './pages/FindAccount';
import Register from './pages/Register';
import Main from './pages/Main';
import Task from './pages/Task';
import Chat from './pages/Chat';
import Settings from './pages/Settings';
import { NotificationProvider } from './NotificationContext';
import GlobalNotificationDisplay from './GlobalNotificationDisplay';

const AuthContext = createContext(null);

const AuthProvider = ({ children }) => {
  const [currentUser, setCurrentUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const checkAuthStatus = async () => {
      setIsLoading(true);
      try {
        const response = await fetch('/api/users/me', { credentials: 'include' });
        if (response.ok) {
          const contentType = response.headers.get("content-type");
          if (contentType && contentType.includes("application/json")) {
            const userData = await response.json();
            setCurrentUser(userData);
          } else {
            setCurrentUser(null);
          }
        } else {
          setCurrentUser(null);
        }
      } catch (error) {
        console.error("인증 상태 확인 오류:", error);
        setCurrentUser(null);
      } finally {
        setIsLoading(false);
      }
    };
    checkAuthStatus();
  }, []);

  const login = (userData) => {
    setCurrentUser(userData);
  };

  const logout = async () => {
    // setCurrentUser(null) 호출 시 NotificationContext의 useEffect가
    // 자동으로 웹소켓 연결을 해제하므로 직접 호출할 필요가 없습니다.
    await fetch('/logout', { method: 'POST', credentials: 'include' });
    setCurrentUser(null);
    navigate('/login');
  };

  const value = { currentUser, login, logout, isLoading };

  return (
      <AuthContext.Provider value={value}>
        {children}
      </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);

const ProtectedRoute = () => {
  const { currentUser, isLoading } = useAuth();
  if (isLoading) return <div>애플리케이션 로딩 중...</div>;
  return currentUser ? <Outlet /> : <Navigate to="/login" replace />;
};

function App() {
  return (
      <AuthProvider>
        <NotificationProvider>
          <div className="App">
            <GlobalNotificationDisplay />
            <Routes>
              <Route path="/" element={<Login />} />
              <Route path="/login" element={<Login />} />
              <Route path="/find-account" element={<FindAccount />} />
              <Route path="/register" element={<Register />} />

              <Route element={<ProtectedRoute />}>
                <Route path="/main" element={<Main />} />
                <Route path="/task" element={<Task />} />
                <Route path="/chat" element={<Chat />} />
                <Route path="/settings" element={<Settings />} />
              </Route>
            </Routes>
          </div>
        </NotificationProvider>
      </AuthProvider>
  );
}

export default App;
