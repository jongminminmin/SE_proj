import React, { useState, useEffect, useRef } from 'react';

// 아이콘 SVG 컴포넌트
const Send = ({ className }) => <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" /></svg>;
const MoreVertical = ({ className }) => <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><circle cx="12" cy="12" r="1"/><circle cx="12" cy="5" r="1"/><circle cx="12" cy="19" r="1"/></svg>;
const Hash = ({ className }) => <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><line x1="4" y1="9" x2="20" y2="9"/><line x1="4" y1="15" x2="20" y2="15"/><line x1="10" y1="3" x2="8" y2="21"/><line x1="16" y1="3" x2="14" y2="21"/></svg>;
const Trash = ({ className }) => <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>;

// --- ▼▼▼ 안정적인 CSS 아바타 컴포넌트 추가 ▼▼▼ ---
const Avatar = ({ user, isOwnMessage }) => {
    // 만약 실제 이미지 URL이 있다면 그것을 사용합니다.
    if (user?.avatar) {
        // 이미지 로드 실패 시, 깨진 아이콘 대신 아무것도 표시하지 않도록 처리합니다.
        return <img src={user.avatar} alt="avatar" className="message-avatar" onError={(e) => { e.target.style.display = 'none'; e.target.onerror = null; }} />;
    }
    // 이미지 URL이 없다면 CSS로 아바타를 생성합니다.
    const initial = (user?.username || '?').charAt(0).toUpperCase();
    const style = {
        width: '40px',
        height: '40px',
        borderRadius: '50%',
        backgroundColor: isOwnMessage ? '#3B82F6' : '#6B7280', // Tailwind's blue-500 and gray-500
        color: 'white',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontWeight: 'bold',
        fontSize: '1rem',
        flexShrink: 0
    };
    return <div style={style} className="message-avatar">{initial}</div>;
};
// --- ▲▲▲ 안정적인 CSS 아바타 컴포넌트 추가 완료 ▲▲▲ ---

const MessageContainer = ({ roomId, messages, sendMessage, roomDetails, currentUser, setChatMessages, onDeleteRoom }) => {
    const [newMessage, setNewMessage] = useState('');
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const messagesEndRef = useRef(null);
    const menuRef = useRef(null);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (menuRef.current && !menuRef.current.contains(event.target)) {
                setIsMenuOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => {
            document.removeEventListener("mousedown", handleClickOutside);
        };
    }, [menuRef]);

    const handleSendMessage = (e) => {
        e.preventDefault();
        const content = newMessage.trim();
        if (content && roomId && currentUser) {
            const optimisticMessage = {
                messageId: `temp-${Date.now()}`,
                chatRoomId: roomId,
                senderId: currentUser.id,
                username: currentUser.username,
                avatar: currentUser.avatar,
                content: content,
                timestamp: new Date().toISOString(),
            };
            setChatMessages(prev => ({ ...prev, [roomId]: [...(prev[roomId] || []), optimisticMessage] }));
            sendMessage(roomId, content);
            setNewMessage('');
        }
    };

    const handleDeleteClick = () => {
        setIsMenuOpen(false);
        // --- ▼▼▼ 오류 방지를 위한 방어 코드 추가 ▼▼▼ ---
        if (typeof onDeleteRoom === 'function') {
            onDeleteRoom(roomId);
        } else {
            console.error("onDeleteRoom prop is not a function. Make sure it is passed correctly from Chat.js");
            alert("채팅방 삭제 기능에 오류가 있습니다. 개발자 콘솔을 확인해주세요.");
        }
        // --- ▲▲▲ 오류 방지를 위한 방어 코드 추가 완료 ▲▲▲ ---
    };

    if (!roomId || !roomDetails) {
        return (
            <div className="main-chat">
                <div className="chat-header" />
                <div className="messages-container placeholder">
                    <span>왼쪽에서 채팅방을 선택하거나, '새 채팅' 버튼으로 대화를 시작하세요.</span>
                </div>
            </div>
        );
    }

    if (!currentUser) return null;

    return (
        <div className="main-chat">
            <div className="chat-header">
                <div className="header-left">
                    <div className="current-room-avatar" style={{backgroundColor: roomDetails.color || '#6c757d'}}><Hash className="room-icon"/></div>
                    <div className="current-room-info">
                        <h3>{roomDetails.name}</h3>
                        <p className="current-room-participants">{roomDetails.participants}명 참여중</p>
                    </div>
                </div>
                <div className="header-actions" ref={menuRef}>
                    {!roomDetails.isTerminated && (
                        <button className="action-button" onClick={() => setIsMenuOpen(prev => !prev)}>
                            <MoreVertical className="action-icon"/>
                        </button>
                    )}
                    {isMenuOpen && (
                        <div className="header-dropdown-menu">
                            <button className="dropdown-item delete" onClick={handleDeleteClick}>
                                <Trash className="dropdown-icon" />
                                채팅방 나가기
                            </button>
                        </div>
                    )}
                </div>
            </div>
            <div className="messages-container">
                {messages.map((msg, index) => {
                    const isOwnMessage = msg.senderId === currentUser.id;
                    return (
                        <div key={msg.messageId || `msg-${index}`} className={`message ${isOwnMessage ? 'own' : 'other'}`}>
                            <div className="message-content">
                                {/* --- ▼▼▼ 아바타를 새 컴포넌트로 교체 ▼▼▼ --- */}
                                {!isOwnMessage && <Avatar user={msg} isOwnMessage={false} />}
                                <div className="message-body">
                                    {!isOwnMessage && <div className="message-sender">{msg.username}</div>}
                                    <div className={`message-bubble ${isOwnMessage ? 'own-bubble' : 'other-bubble'}`}><p className="message-text">{msg.content}</p></div>
                                    <div className="message-footer"><div className="message-time">{new Date(msg.timestamp).toLocaleTimeString('ko-KR', { hour: 'numeric', minute: '2-digit' })}</div></div>
                                </div>
                                {isOwnMessage && <Avatar user={currentUser} isOwnMessage={true} />}
                                {/* --- ▲▲▲ 아바타를 새 컴포넌트로 교체 완료 ▲▲▲ --- */}
                            </div>
                        </div>
                    );
                })}
                <div ref={messagesEndRef} />
            </div>
            <form className="message-input-container" onSubmit={handleSendMessage}>
                <div className="message-input-wrapper">
                    <textarea value={newMessage} onChange={(e) => setNewMessage(e.target.value)} onKeyPress={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSendMessage(e); } }} placeholder="메시지 보내기..." className="message-input" rows="1" />
                    <button type="submit" disabled={!newMessage.trim()} className={`send-button ${!newMessage.trim() ? 'disabled' : 'enabled'}`}><Send className="send-icon"/></button>
                </div>
            </form>
        </div>
    );
};

export default MessageContainer;
