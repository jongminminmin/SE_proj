import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Home, MessageCircle, Settings, LogOut, User } from 'lucide-react';
import { useNotifications } from '../NotificationContext';
import './Sidebar.css';

const Sidebar = ({ user, onLogout }) => {
    const navigate = useNavigate();
    const { globalUnreadCount } = useNotifications(); // updateGlobalUnreadCount 호출 제거

    const handleLogout = async () => {
        if (onLogout) {
            onLogout();
        }
        navigate('/login');
    };

    return (
        <div className="sidebar">
            <div className="sidebar-header">
                <Link to="/main" className="sidebar-logo">MATE</Link>
            </div>
            <nav className="sidebar-nav">
                <Link to="/main" className="sidebar-item"><Home size={20} /><span>메인</span></Link>
                <Link to="/chat" className="sidebar-item">
                    <MessageCircle size={20} />
                    <span>채팅</span>
                    {globalUnreadCount > 0 && (<span className="unread-badge">{globalUnreadCount}</span>)}
                </Link>
            </nav>
            <div className="sidebar-footer">
                <div className="sidebar-user-info">
                    <div className="sidebar-user-avatar"><User size={24} /></div>
                    <span className="sidebar-username">{user?.username || 'Guest'}</span>
                </div>
                <Link to="/settings" className="sidebar-item"><Settings size={20} /><span>설정</span></Link>
                <button onClick={handleLogout} className="sidebar-item sidebar-logout-button"><LogOut size={20} /><span>로그아웃</span></button>
            </div>
        </div>
    );
};

export default Sidebar;
