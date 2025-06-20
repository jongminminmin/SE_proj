// 파일 경로: /src/pages/Chat.js
// 이 파일의 전체 내용을 아래 코드로 교체해주세요.

import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useLocation, useNavigate} from "react-router-dom";
import {createPortal} from 'react-dom';
import {useAuth} from '../App';
import {useNotifications} from '../NotificationContext';
import './Chat.css';

// 아이콘 SVG 컴포넌트들
const Send = ({ className }) => <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" /></svg>;
const Smile = ({ className }) => <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><path d="m9 9 6 0"/><path d="m9 15 6 0"/></svg>;
const MoreVertical = ({ className }) => <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><circle cx="12" cy="12" r="1"/><circle cx="12" cy="5" r="1"/><circle cx="12" cy="19" r="1"/></svg>;
const Hash = ({ className }) => <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><line x1="4" y1="9" x2="20" y2="9"/><line x1="4" y1="15" x2="20" y2="15"/><line x1="10" y1="3" x2="8" y2="21"/><line x1="16" y1="3" x2="14" y2="21"/></svg>;
const Users = ({ className }) => <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>;

// 사용자 목록 모달 컴포넌트
const UserListModal = ({ isOpen, onClose, users, onUserClick }) => {
    if (!isOpen) return null;
    return createPortal(
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content user-list-modal" onClick={e => e.stopPropagation()}>
                <div className="modal-header">
                    <h3>전체 사용자 목록</h3>
                    <button onClick={onClose} className="modal-close-btn">&times;</button>
                </div>
                <div className="modal-body">
                    {users.map(user => (
                        <div key={user.id} className="user-list-item" onClick={() => onUserClick(user)}>
                            <div className="user-avatar-container">
                                <img src={user.avatar} alt={user.username} className="user-avatar" />
                                <div className={`user-status-indicator ${user.status}`}></div>
                            </div>
                            <span className="user-name">{user.username}</span>
                            <span className={`user-status-text ${user.status}`}>{user.status === 'online' ? '온라인' : '오프라인'}</span>
                        </div>
                    ))}
                    {users.length === 0 && <p className="no-users-text">사용자 정보가 없습니다.</p>}
                </div>
            </div>
        </div>,
        document.body
    );
};


const Chat = () => {
    const { currentUser } = useAuth();
    const { stompClient, isConnected, updateGlobalUnreadCount } = useNotifications();
    const navigate = useNavigate();
    const location = useLocation();

    const [showUserListModal, setShowUserListModal] = useState(false);
    const [messages, setMessages] = useState({});
    const [newMessage, setNewMessage] = useState('');
    const [chatRooms, setChatRooms] = useState([]);
    const [currentChatRoom, setCurrentChatRoom] = useState(null);
    const [allUsers, setAllUsers] = useState([]);
    const [connectedUsernames, setConnectedUsernames] = useState(new Set());

    const messagesEndRef = useRef(null);
    const localSubscriptions = useRef({});
    // [1차 수정] 현재 채팅방 ID를 ref로 관리하여 항상 최신 값을 참조하도록 함
    const currentChatRoomRef = useRef(currentChatRoom);
    useEffect(() => {
        currentChatRoomRef.current = currentChatRoom;
    }, [currentChatRoom]);

    const usersWithStatus = useMemo(() => {
        if (!allUsers.length) return [];
        return allUsers.map(user => ({
            ...user,
            avatar: user.avatar || `https://via.placeholder.com/40/CCCCCC/FFFFFF/?text=${user.username.charAt(0).toUpperCase()}`,
            status: connectedUsernames.has(user.username) ? 'online' : 'offline'
        })).sort((a, b) => {
            if (a.status === 'online' && b.status !== 'online') return -1;
            if (a.status !== 'online' && b.status === 'online') return 1;
            return a.username.localeCompare(b.username);
        });
    }, [allUsers, connectedUsernames]);

    useEffect(() => {
        if (!currentUser) return;
        const fetchInitialData = async () => {
            try {
                const [usersRes, connectedRes] = await Promise.all([ fetch('/api/users'), fetch('/api/users/connected') ]);
                if (usersRes.ok) setAllUsers(await usersRes.json());
                if (connectedRes.ok) setConnectedUsernames(new Set(await connectedRes.json()));
            } catch (e) { console.error("초기 사용자 데이터 로딩 실패:", e); }
        };
        fetchInitialData();
    }, [currentUser]);

    useEffect(() => {
        if (!isConnected || !stompClient) return;
        const sub = stompClient.subscribe('/topic/connectedUsers', (message) => {
            setConnectedUsernames(new Set(JSON.parse(message.body)));
        });
        return () => { if (sub) sub.unsubscribe(); };
    }, [isConnected, stompClient]);

    const handleUserClickForChat = async (user) => {
        if (user.id === currentUser.id) return;
        setShowUserListModal(false);
        try {
            const response = await fetch('/api/chats/create-private-room', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ user1Id: currentUser.id, user2Id: user.id })
            });
            if (response.ok) {
                const newRoom = await response.json();
                if (!chatRooms.some(room => room.id === newRoom.id)) {
                    setChatRooms(prev => [...prev, newRoom]);
                }
                handleRoomChange(newRoom.id);
            } else { console.error("1:1 채팅방 생성 실패:", await response.text()); }
        } catch (error) { console.error("1:1 채팅방 생성 중 오류:", error); }
    };

    useEffect(() => {
        const queryParams = new URLSearchParams(location.search);
        const roomIdFromQuery = queryParams.get('roomId');
        if (roomIdFromQuery && roomIdFromQuery !== currentChatRoom) {
            setCurrentChatRoom(roomIdFromQuery);
        }
    }, [location.search, currentChatRoom]);

    const fetchChatRooms = useCallback(async () => {
        try {
            const response = await fetch('/api/chats/my-rooms');
            if (response.ok) {
                const roomsData = await response.json();
                setChatRooms(roomsData || []);
                const queryParams = new URLSearchParams(window.location.search);
                const roomIdFromQuery = queryParams.get('roomId');
                if (roomsData && roomsData.length > 0) {
                    const targetRoomId = roomIdFromQuery && roomsData.some(r => r.id === roomIdFromQuery) ? roomIdFromQuery : roomsData[0].id;
                    if (targetRoomId !== currentChatRoom) {
                        setCurrentChatRoom(targetRoomId);
                        navigate(`/chat?roomId=${targetRoomId}`, { replace: true });
                    }
                }
            }
        } catch (error) { console.error("채팅방 목록 로딩 실패:", error); }
    }, [currentChatRoom, navigate]);

    useEffect(() => { if (currentUser) { fetchChatRooms(); } }, [currentUser, fetchChatRooms]);

    const fetchHistoryAndMarkAsRead = useCallback(async (roomId) => {
        if (!roomId || !currentUser) return;
        try {
            const historyRes = await fetch(`/api/chats/rooms/${roomId}/messages`);
            if (historyRes.ok) {
                const historyData = await historyRes.json();
                const formatted = (historyData || []).map(msg => ({ ...msg, id: msg.messageId, isOwn: msg.senderId === currentUser.id }));
                setMessages(prev => ({ ...prev, [roomId]: formatted }));
            }
            await fetch(`/api/chats/rooms/${roomId}/mark-as-read`, { method: 'POST' });
            updateGlobalUnreadCount();
        } catch (error) { console.error(`채팅방 ${roomId} 데이터 로딩 실패:`, error); }
    }, [currentUser, updateGlobalUnreadCount]);

    useEffect(() => { if (currentChatRoom) { fetchHistoryAndMarkAsRead(currentChatRoom); } }, [currentChatRoom, fetchHistoryAndMarkAsRead]);

    // [2차 수정] 메시지 수신 로직 수정
    useEffect(() => {
        if (!isConnected || !stompClient || !currentUser || !Array.isArray(chatRooms)) return;

        chatRooms.forEach(room => {
            const subscriptionPath = `/topic/${room.type === 'PRIVATE' ? 'private' : 'group'}/${room.intId}`;
            if (!localSubscriptions.current[room.id]) {
                const sub = stompClient.subscribe(subscriptionPath, (message) => {
                    const receivedMsg = JSON.parse(message.body);

                    if (receivedMsg.senderId === currentUser.id) return;

                    // ref를 사용해 현재 채팅방 ID와 비교하여 다른 클라이언트의 메시지를 즉시 반영
                    if (receivedMsg.chatRoomId === currentChatRoomRef.current) {
                        const messageToAdd = { ...receivedMsg, id: receivedMsg.messageId, isOwn: false };
                        setMessages(prev => ({ ...prev, [receivedMsg.chatRoomId]: [...(prev[receivedMsg.chatRoomId] || []), messageToAdd] }));
                        fetch(`/api/chats/rooms/${receivedMsg.chatRoomId}/mark-as-read`, { method: 'POST' }).then(() => updateGlobalUnreadCount());
                    }
                });
                localSubscriptions.current[room.id] = sub;
            }
        });

        return () => {
            Object.values(localSubscriptions.current).forEach(sub => sub.unsubscribe());
            localSubscriptions.current = {};
        };
    }, [isConnected, stompClient, currentUser, chatRooms, updateGlobalUnreadCount]);


    useEffect(() => { messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages]);

    // [3차 수정] 메시지 전송 로직 수정 (낙관적 업데이트)
    const handleSendMessage = () => {
        if (!newMessage.trim() || !currentUser || !stompClient?.active || !currentChatRoom) return;

        const timestamp = new Date().toISOString();
        const content = newMessage.trim();
        const tempId = `temp_${Date.now()}`;

        const tempMessage = {
            id: tempId,
            messageId: tempId,
            chatRoomId: currentChatRoom,
            senderId: currentUser.id,
            username: currentUser.username,
            content: content,
            timestamp: timestamp,
            isOwn: true,
            avatar: currentUser?.avatar || `https://via.placeholder.com/40/3B82F6/FFFFFF/?text=${(currentUser.username || 'M').charAt(0)}`,
        };

        setMessages(prev => ({ ...prev, [currentChatRoom]: [...(prev[currentChatRoom] || []), tempMessage] }));
        setNewMessage('');

        const { id, isOwn, avatar, ...payload } = tempMessage;
        stompClient.publish({
            destination: `/app/chat.sendMessage/${currentChatRoom}`,
            body: JSON.stringify(payload)
        });
    };

    const handleRoomChange = (roomId) => {
        if (roomId !== currentChatRoom) {
            setMessages(prev => ({ ...prev, [roomId]: [] }));
            setCurrentChatRoom(roomId);
            navigate(`/chat?roomId=${roomId}`, { replace: true });
        }
    };

    return (
        <>
            <div className="chat-container">
                <div className="left-sidebar">
                    <div className="sidebar-header">
                        <h2 className="sidebar-title">프로젝트 채팅</h2>
                        <div className="connection-status">
                            <div className={`status-indicator ${isConnected ? 'connected' : 'disconnected'}`}></div>
                            <span className="status-text">{isConnected ? '연결됨' : '연결 중...'}</span>
                        </div>
                    </div>
                    <div className="rooms-list">
                        {chatRooms.map(room => (
                            <div key={room.id} className={`room-item ${currentChatRoom === room.id ? 'active' : ''}`} onClick={() => handleRoomChange(room.id)}>
                                <div className="room-content">
                                    <div className="room-avatar" style={{backgroundColor: room.color || '#6c757d'}}><Hash className="room-icon" /></div>
                                    <div className="room-info">
                                        <div className="room-header">
                                            <h4 className="room-name">{room.name}</h4>
                                            {room.unreadCount > 0 && currentChatRoom !== room.id && (<span className="unread-badge">{room.unreadCount}</span>)}
                                        </div>
                                        <p className="room-last-message">{room.lastMessage || '메시지가 없습니다.'}</p>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
                <div className="main-chat">
                    <div className="chat-header">
                        <div className="header-left">
                            <div className="current-room-avatar" style={{backgroundColor: chatRooms.find(r=>r.id===currentChatRoom)?.color || '#6c757d'}}><Hash className="room-icon"/></div>
                            <div className="current-room-info">
                                <h3 className="current-room-name">{chatRooms.find(r=>r.id===currentChatRoom)?.name || '채팅방을 선택하세요'}</h3>
                                <p className="current-room-participants">{chatRooms.find(r=>r.id===currentChatRoom)?.participants ?? 0}명 참여 중</p>
                            </div>
                        </div>
                        <div className="header-actions">
                            <button className="action-button" onClick={() => setShowUserListModal(true)}><Users className="action-icon"/></button>
                            <button className="action-button"><MoreVertical className="action-icon"/></button>
                        </div>
                    </div>
                    <div className="messages-container">
                        {(messages[currentChatRoom] || []).map((msg) => (
                            <div key={msg.id} className={`message ${msg.isOwn ? 'own' : 'other'}`}>
                                <div className="message-content">
                                    {!msg.isOwn && <img src={msg.avatar || `https://via.placeholder.com/40/78716C/FFFFFF/?text=${(msg.username || 'U').charAt(0)}`} alt="avatar" className="message-avatar" />}
                                    <div className="message-body">
                                        {!msg.isOwn && <div className="message-sender">{msg.username || msg.sender}</div>}
                                        <div className={`message-bubble ${msg.isOwn ? 'own-bubble' : 'other-bubble'}`}><p className="message-text">{msg.content}</p></div>
                                        <div className="message-footer"><div className="message-time">{new Date(msg.timestamp).toLocaleTimeString('ko-KR', { hour: 'numeric', minute: '2-digit' })}</div></div>
                                    </div>
                                    {msg.isOwn && <img src={currentUser?.avatar || `https://via.placeholder.com/40/3B82F6/FFFFFF/?text=${(currentUser.username || 'M').charAt(0)}`} alt="avatar" className="message-avatar" />}
                                </div>
                            </div>
                        ))}
                        <div ref={messagesEndRef} />
                    </div>
                    <div className="message-input-container">
                        <div className="message-input-wrapper">
                            <textarea value={newMessage} onChange={(e) => setNewMessage(e.target.value)} onKeyPress={(e) => e.key === 'Enter' && !e.shiftKey && (e.preventDefault(), handleSendMessage())} placeholder="메시지 보내기..." className="message-input" rows="1" disabled={!currentChatRoom} />
                            <button className="input-action-button" disabled={!currentChatRoom}><Smile className="input-action-icon"/></button>
                            <button onClick={handleSendMessage} disabled={!newMessage.trim() || !currentChatRoom} className={`send-button ${!newMessage.trim() || !currentChatRoom ? 'disabled' : 'enabled'}`}><Send className="send-icon"/></button>
                        </div>
                    </div>
                </div>
            </div>
            <UserListModal isOpen={showUserListModal} onClose={() => setShowUserListModal(false)} users={usersWithStatus} onUserClick={handleUserClickForChat} />
        </>
    );
};

export default Chat;

