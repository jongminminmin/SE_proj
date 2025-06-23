import React from 'react';
import './ChatRoomList.css'; // 스타일을 위해 CSS 파일을 임포트합니다.

// 아이콘 SVG 컴포넌트
const UserPlus = ({ className }) => <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2m7-10a4 4 0 100-8 4 4 0 000 8zm8 2a2 2 0 012 2v2m-2-4h-4m4 0v4m-4-4v-4m4 4h4"></path></svg>;
const Hash = ({ className }) => <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><line x1="4" y1="9" x2="20" y2="9"/><line x1="4" y1="15" x2="20" y2="15"/><line x1="10" y1="3" x2="8" y2="21"/><line x1="16" y1="3" x2="14" y2="21"/></svg>;

const ChatRoomList = ({ rooms, currentRoomId, onRoomChange, isConnected, onNewChat, currentUser }) => {
    return (
        <aside className="chat-sidebar">
            <div className="sidebar-header">
                <h2>프로젝트 채팅</h2>
                <button className="new-chat-btn" title="새 대화 시작" onClick={onNewChat}>
                    <UserPlus className="new-chat-icon" />
                </button>
            </div>

            <div className="connection-status-container">
                <div className="connection-status">
                    <span className={`status-dot ${isConnected ? 'connected' : 'disconnected'}`}></span>
                    <span>{isConnected ? '연결됨' : '연결 끊김'}</span>
                </div>
            </div>

            <div className="room-list">
                {rooms.map(room => (
                    <div
                        key={room.id}
                        className={`room-item ${currentRoomId === room.id ? 'active' : ''} ${room.isTerminated ? 'terminated' : ''}`}
                        onClick={() => !room.isTerminated && onRoomChange(room.id)}
                    >
                        <div className="room-avatar" style={{ backgroundColor: room.isTerminated ? '#adb5bd' : (room.color || '#6c757d') }}>
                            <Hash className="room-icon" />
                        </div>
                        <div className="room-info">
                            <span className="room-name">{room.name}</span>
                            <p className="last-message">{room.lastMessage || '대화를 시작해보세요.'}</p>
                        </div>
                        {!room.isTerminated && room.unreadCount > 0 && <span className="unread-badge">{room.unreadCount}</span>}
                    </div>
                ))}
            </div>

            {/* 요청하신 사용자 프로필 표시 영역(sidebar-footer)이
        여기에서 완전히 삭제되었습니다.
      */}
        </aside>
    );
};

export default ChatRoomList;