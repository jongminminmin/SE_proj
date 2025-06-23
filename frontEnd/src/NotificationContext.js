import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { useAuth } from './App';
import * as stompService from './services/stompService';

const NotificationContext = createContext(null);
export const useNotifications = () => useContext(NotificationContext);

export const NotificationProvider = ({ children }) => {
    const { currentUser } = useAuth();
    const [isConnected, setIsConnected] = useState(stompService.getStompClient()?.active || false);
    const [chatRooms, setChatRooms] = useState([]);
    const [chatMessages, setChatMessages] = useState({});
    const [globalUnreadCount, setGlobalUnreadCount] = useState(0);
    const [latestMessageNotification, setLatestMessageNotification] = useState(null);

    const onMessageReceivedRef = useRef(null);
    const onSystemNotificationReceivedRef = useRef(null);

    const onSystemNotificationReceived = useCallback((payload) => {
        const notification = JSON.parse(payload.body);
        if (notification.type === 'ROOM_DELETED') {
            setChatRooms(prevRooms =>
                prevRooms.map(room =>
                    room.id === notification.roomId
                        ? { ...room, isTerminated: true, lastMessage: '상대방이 대화를 종료했습니다.', unreadCount: 0 }
                        : room
                )
            );
        }
    }, []);

    const onMessageReceived = useCallback((payload) => {
        const receivedMsg = JSON.parse(payload.body);
        const { chatRoomId } = receivedMsg;

        setChatMessages(prev => {
            const currentRoomMessages = prev[chatRoomId] || [];
            if (currentRoomMessages.some(msg => msg.messageId === receivedMsg.messageId)) return prev;
            const optimisticMessage = currentRoomMessages.find(m => m.messageId?.startsWith('temp-'));
            const baseMessages = optimisticMessage ? currentRoomMessages.filter(m => m.messageId !== optimisticMessage.messageId) : currentRoomMessages;
            return { ...prev, [chatRoomId]: [...baseMessages, receivedMsg] };
        });

        const isChatPageActive = window.location.pathname.includes('/chat') && new URLSearchParams(window.location.search).get('roomId') === chatRoomId;

        setChatRooms(prevRooms => {
            const newRooms = prevRooms.map(room => {
                if (room.id === chatRoomId) {
                    return { ...room, lastMessage: receivedMsg.content, lastMessageTime: receivedMsg.timestamp, unreadCount: (receivedMsg.senderId !== currentUser?.id && !isChatPageActive) ? (room.unreadCount || 0) + 1 : room.unreadCount };
                }
                return room;
            });
            return newRooms.sort((a, b) => new Date(b.lastMessageTime) - new Date(a.lastMessageTime));
        });

        if (receivedMsg.senderId !== currentUser?.id && !isChatPageActive) {
            setLatestMessageNotification({ roomId: receivedMsg.chatRoomId, sender: receivedMsg.username, content: receivedMsg.content });
        }
    }, [currentUser?.id]);

    useEffect(() => {
        onMessageReceivedRef.current = onMessageReceived;
        onSystemNotificationReceivedRef.current = onSystemNotificationReceived;
    });

    useEffect(() => {
        const unsubscribeConnect = stompService.addConnectListener(() => setIsConnected(true));
        const unsubscribeDisconnect = stompService.addDisconnectListener(() => {
            setIsConnected(false);
            setChatRooms([]);
            setChatMessages({});
        });
        return () => {
            unsubscribeConnect();
            unsubscribeDisconnect();
        };
    }, []);

    useEffect(() => {
        if (isConnected && currentUser) {
            fetch('/api/chats/my-rooms').then(res => res.json()).then(rooms => setChatRooms(rooms || []));
            const userTopic = `/topic/user/${currentUser.id}`;
            stompService.subscribe(userTopic, (payload) => onSystemNotificationReceivedRef.current?.(payload));
        }
    }, [isConnected, currentUser]);

    useEffect(() => {
        if (isConnected) {
            chatRooms.forEach(room => {
                if (room.isTerminated) return;
                const destination = `/topic/${room.type.toLowerCase()}/${room.intId}`;
                stompService.subscribe(destination, (payload) => onMessageReceivedRef.current?.(payload));
            });
        }
    }, [isConnected]);

    // --- ▼▼▼ 여기가 핵심 수정 부분 ▼▼▼ ---

    // Main.js에서 호출할 수 있도록, 채팅방 목록이 바뀔 때마다
    // 전역 읽지 않은 메시지 수를 계산하는 함수를 정의합니다.
    const updateGlobalUnreadCount = useCallback(() => {
        const count = chatRooms.reduce((total, room) => total + (room.unreadCount || 0), 0);
        setGlobalUnreadCount(count);
    }, [chatRooms]);

    // chatRooms 상태가 변경될 때마다 updateGlobalUnreadCount 함수를 호출하여
    // globalUnreadCount를 자동으로 업데이트합니다.
    useEffect(() => {
        updateGlobalUnreadCount();
    }, [chatRooms]);

    // --- ▲▲▲ 핵심 수정 완료 ▲▲▲ ---

    const sendMessage = useCallback((roomId, messageContent) => {
        stompService.publish({
            destination: `/app/chat.sendMessage/${roomId}`,
            body: JSON.stringify({ chatRoomId: roomId, content: messageContent })
        });
    }, []);

    const markRoomAsRead = useCallback((roomId) => {
        setChatRooms(prevRooms =>
            prevRooms.map(r => r.id === roomId && r.unreadCount > 0 ? { ...r, unreadCount: 0 } : r)
        );
    }, []);

    // --- 컨텍스트로 전달할 값에 updateGlobalUnreadCount를 추가합니다. ---
    const value = {
        isConnected,
        chatMessages,
        setChatMessages,
        chatRooms,
        setChatRooms,
        globalUnreadCount,
        updateGlobalUnreadCount, // 이 함수를 추가
        sendMessage,
        markRoomAsRead,
        latestMessageNotification,
        clearLatestNotification: () => setLatestMessageNotification(null)
    };

    return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
};
