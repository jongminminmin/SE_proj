import React, { useCallback, useEffect, useState, useMemo } from 'react';
import { useLocation, useNavigate } from "react-router-dom";

import { useAuth } from '../App';
import { useNotifications } from '../NotificationContext';
import ChatRoomList from '../components/ChatRoomList';
import MessageContainer from '../components/MessageContainer';
import { CreateChatModal } from '../components/CreateChatModal';
import './Chat.css';

const Chat = () => {
    const { currentUser } = useAuth();
    const { isConnected, chatMessages, setChatMessages, chatRooms, setChatRooms, sendMessage, markRoomAsRead } = useNotifications();
    const [allUsers, setAllUsers] = useState([]);
    const [isModalOpen, setIsModalOpen] = useState(false);

    const navigate = useNavigate();
    const location = useLocation();

    // URL에서 roomId를 추출하는 것을 유일한 정보 소스로 사용합니다.
    const currentChatRoomId = useMemo(() => new URLSearchParams(location.search).get('roomId'), [location.search]);

    // 채팅방 목록 상태와 URL을 동기화하는 로직
    useEffect(() => {
        // 1. 채팅방 목록이 유효하게 로드되었고, 1개 이상 존재하는 경우
        if (chatRooms && chatRooms.length > 0) {
            const roomExists = chatRooms.some(room => room.id === currentChatRoomId);

            // 1-1. 현재 선택된 방이 없거나, 더 이상 존재하지 않는 방인 경우
            if (!currentChatRoomId || !roomExists) {
                // 안전하게 목록의 첫 번째 방으로 URL을 변경하여 이동시킵니다.
                navigate(`/chat?roomId=${chatRooms[0].id}`, { replace: true });
            }
        }
    }, [chatRooms, currentChatRoomId, navigate]);

    // 메시지 히스토리 로딩 로직
    useEffect(() => {
        // 현재 채팅방 ID가 있고, 해당 방의 메시지 기록이 로컬에 없을 때만 실행
        if (currentChatRoomId && !chatMessages[currentChatRoomId]) {
            const fetchHistory = async () => {
                try {
                    const response = await fetch(`/api/chats/rooms/${currentChatRoomId}/messages`);
                    if (response.ok) {
                        const historyData = await response.json();
                        setChatMessages(prev => ({ ...prev, [currentChatRoomId]: historyData || [] }));
                    } else if (response.status !== 404) { // 'Not Found' 오류는 무시
                        console.error(`채팅 기록 로딩 오류: ${response.statusText}`);
                    }
                } catch (error) {
                    console.error("채팅 기록 로딩 중 네트워크 오류:", error);
                }
            };
            fetchHistory();
            markRoomAsRead(currentChatRoomId);
        }
    }, [currentChatRoomId, chatMessages, setChatMessages, markRoomAsRead]);

    // 전체 사용자 목록 로딩
    const fetchAllUsers = useCallback(async () => {
        if (!currentUser) return;
        try {
            const response = await fetch('/api/users');
            if (response.ok) {
                const usersData = await response.json();
                setAllUsers(usersData.filter(u => u.id !== currentUser.id));
            }
        } catch (error) { console.error("전체 사용자 목록 로딩 오류:", error); }
    }, [currentUser]);

    useEffect(() => { fetchAllUsers(); }, [fetchAllUsers]);

    // 채팅방 삭제 핸들러
    const handleDeleteRoom = async (roomIdToDelete) => {
        if (!roomIdToDelete) return;

        if (window.confirm("정말로 이 채팅방을 나가시겠습니까?")) {
            try {
                const response = await fetch(`/api/chats/rooms/${roomIdToDelete}`, { method: 'DELETE' });

                if (response.ok || response.status === 204) {
                    const remainingRooms = chatRooms.filter(room => room.id !== roomIdToDelete);
                    const nextRoomId = remainingRooms.length > 0 ? remainingRooms[0].id : null;

                    setChatRooms(remainingRooms);

                    setChatMessages(prevMessages => {
                        const newMessages = { ...prevMessages };
                        delete newMessages[roomIdToDelete];
                        return newMessages;
                    });

                    if (nextRoomId) {
                        navigate(`/chat?roomId=${nextRoomId}`, { replace: true });
                    } else {
                        navigate('/chat', { replace: true });
                    }
                } else {
                    alert('채팅방 삭제에 실패했습니다.');
                }
            } catch (error) {
                alert('채팅방 삭제 중 오류가 발생했습니다.');
            }
        }
    };

    // --- ▼▼▼ 채팅방 생성 핸들러 (핵심 수정) ▼▼▼ ---
    const handleCreateChatRoom = async (creationData) => {
        if (!currentUser) {
            alert("사용자 정보가 없습니다. 다시 로그인 해주세요.");
            return;
        }

        try {
            const payload = {
                type: creationData.type,
                ...(creationData.type === 'PRIVATE'
                        ? { user2Id: creationData.user2Id }
                        : {
                            name: creationData.name,
                            description: `${creationData.name} 그룹 채팅방`,
                            participants: [currentUser.id, ...creationData.participants]
                        }
                )
            };

            const response = await fetch('/api/chats/create', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                const newRoom = await response.json();

                if (!chatRooms.some(room => room.id === newRoom.id)) {
                    setChatRooms(prevRooms => [newRoom, ...prevRooms]);
                }

                // 새로 생성된 방으로 바로 이동
                navigate(`/chat?roomId=${newRoom.id}`, { replace: true });

            } else {
                const errorData = await response.json().catch(() => ({ message: '서버 응답 처리 실패' }));
                alert(`채팅방 생성 실패: ${errorData.message}`);
            }
        } catch (error) {
            console.error("채팅방 생성 중 오류:", error);
            alert(`채팅방 생성 중 오류가 발생했습니다.`);
        }
    };

    // 채팅방 목록에서 방을 클릭했을 때 호출되는 핸들러
    const handleRoomChange = (roomId) => {
        if (roomId !== currentChatRoomId) {
            navigate(`/chat?roomId=${roomId}`);
        }
    };

    // 현재 선택된 채팅방의 상세 정보와 메시지 목록을 계산
    const currentRoomDetails = useMemo(() => chatRooms.find(r => r.id === currentChatRoomId), [chatRooms, currentChatRoomId]);
    const currentMessages = useMemo(() => chatMessages[currentChatRoomId] || [], [chatMessages, currentChatRoomId]);

    // 사용자 정보가 로딩 중일 때 로딩 화면 표시
    if (!currentUser) {
        return <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}><h2>로딩 중...</h2></div>;
    }

    return (
        <>
            <div className="chat-container">
                <ChatRoomList
                    rooms={chatRooms}
                    currentRoomId={currentChatRoomId}
                    onRoomChange={handleRoomChange}
                    isConnected={isConnected}
                    onNewChat={() => setIsModalOpen(true)}
                    currentUser={currentUser}
                />
                <MessageContainer
                    key={currentChatRoomId} // key를 사용해 방이 바뀔 때마다 컴포넌트를 새로 만듦
                    roomId={currentChatRoomId}
                    messages={currentMessages}
                    sendMessage={sendMessage}
                    roomDetails={currentRoomDetails}
                    setChatMessages={setChatMessages}
                    currentUser={currentUser}
                    onDeleteRoom={handleDeleteRoom}
                    onNewChat={() => setIsModalOpen(true)} // 채팅방 없을 때를 위한 핸들러 전달
                />
            </div>
            <CreateChatModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                allUsers={allUsers}
                onChatRoomCreated={handleCreateChatRoom}
            />
        </>
    );
};

export default Chat;
