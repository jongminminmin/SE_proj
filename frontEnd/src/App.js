import React, { createContext, useContext, useEffect, useState } from 'react';
import { Navigate, Outlet, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
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
import Sidebar from './components/Sidebar';

import * as stompService from './services/stompService'

const AuthContext = createContext(null);

const AuthProvider = ({ children }) => {
  const [currentUser, setCurrentUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  //웹소켓 제어 로직
  useEffect(() => {
    if(currentUser){
      stompService.connect();
    }
    else if( !isLoading){
      stompService.disconnect();
    }
  }, [currentUser, isLoading]);


  useEffect(() => {
    const checkAuthStatus = async () => {
      setIsLoading(true);
      try {
        const response = await fetch('/api/users/me', { credentials: 'include' });
        if (response.ok) {
          const userData = await response.json();
          setCurrentUser(userData);
        } else {
          setCurrentUser(null);
        }
      } catch (error) {
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
    try {
      await fetch('/api/auth/logout', { method: 'POST' });
    } catch (error) {
      console.error('Logout failed:', error);
    } finally {
      setCurrentUser(null);
      navigate('/login');
    }
  };

  const value = { currentUser, login, logout, isLoading };

  return (
      <AuthContext.Provider value={value}>
        {children}
      </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);

const SidebarLayout = () => {
  const { currentUser, logout } = useAuth();
  return (
    <div className="layout-with-sidebar">
      <Sidebar user={currentUser} onLogout={logout} />
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
};

const ProtectedRoute = () => {
  const { currentUser, isLoading } = useAuth();
  if (isLoading) {
    return <div>Loading...</div>;
  }
  
  return currentUser ? <SidebarLayout /> : <Navigate to="/login" replace />;
};

function App() {
  return (
      <AuthProvider>
        <NotificationProvider>
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
        </NotificationProvider>
      </AuthProvider>
  );
}

export default App;
