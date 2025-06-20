import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { useAuth } from './App'; // AuthContext에서 currentUser를 가져오기 위함
import { Client } from '@stomp/stompjs';

// 1. 컨텍스트 생성
const NotificationContext = createContext(null);

// 2. 다른 컴포넌트에서 쉽게 사용할 수 있도록 커스텀 훅 생성
export const useNotifications = () => useContext(NotificationContext);

// 3. Provider 컴포넌트 정의 (웹소켓 로직 포함)
export const NotificationProvider = ({ children }) => {
    const { currentUser } = useAuth();
    const [stompClient, setStompClient] = useState(null);
    const [isConnected, setIsConnected] = useState(false);
    const [notifications, setNotifications] = useState([]);
    const [globalUnreadCount, setGlobalUnreadCount] = useState(0);
    const [latestMessageNotification, setLatestMessageNotification] = useState(null);

    const subscriptions = useRef({});

    // 모든 채팅방의 안 읽은 메시지 수 합산
    const calculateTotalUnreadCount = (rooms) => {
        return rooms.reduce((total, room) => total + (room.unreadCount || 0), 0);
    };

    // 웹소켓 연결 로직
    useEffect(() => {
        if (currentUser && !stompClient) {
            const client = new Client({
                brokerURL: `ws://${window.location.hostname}:9000/ws`,
                connectHeaders: { username: currentUser.username },
                reconnectDelay: 10000,
                heartbeatIncoming: 15000,
                heartbeatOutgoing: 15000,
                onConnect: () => {
                    console.log('전역 웹소켓 연결 성공');
                    setIsConnected(true);

                    // 모든 채팅방에 대한 구독 처리
                    fetch('/api/chats/my-rooms')
                        .then(res => res.json())
                        .then(rooms => {
                            if (!rooms) return;
                            setGlobalUnreadCount(calculateTotalUnreadCount(rooms));
                            rooms.forEach(room => {
                                const subscriptionPath = `/topic/${room.type === 'PRIVATE' ? 'private' : 'group'}/${room.intId}`;
                                if (!subscriptions.current[room.id]) {
                                    const sub = client.subscribe(subscriptionPath, (message) => {
                                        const receivedMsg = JSON.parse(message.body);

                                        // 자신이 보낸 메시지는 알림 처리 안함
                                        if (receivedMsg.senderId === currentUser.id) return;

                                        // 현재 URL이 /chat 이고 해당 채팅방을 보고 있는 경우는 제외
                                        const isChatPageActive = window.location.pathname.includes('/chat') && new URLSearchParams(window.location.search).get('roomId') === receivedMsg.chatRoomId;

                                        if(!isChatPageActive) {
                                            setLatestMessageNotification({
                                                roomId: receivedMsg.chatRoomId,
                                                sender: receivedMsg.username,
                                                content: receivedMsg.content
                                            });
                                            setGlobalUnreadCount(prev => prev + 1);
                                        }
                                    });
                                    subscriptions.current[room.id] = sub;
                                }
                            });
                        });
                },
                onDisconnect: () => {
                    console.log('전역 웹소켓 연결 끊김');
                    setIsConnected(false);
                },
                onStompError: (frame) => {
                    console.error('전역 웹소켓 오류:', frame.headers['message'], frame.body);
                    setIsConnected(false);
                },
            });

            client.activate();
            setStompClient(client);
        } else if (!currentUser && stompClient) {
            stompClient.deactivate();
            setStompClient(null);
            setIsConnected(false);
            console.log('로그아웃으로 인한 전역 웹소켓 연결 해제');
        }

        return () => {
            if (stompClient?.active) {
                stompClient.deactivate();
            }
        };
    }, [currentUser, stompClient]);

    const clearLatestNotification = () => {
        setLatestMessageNotification(null);
    };

    // 외부에서 안 읽은 메시지 수를 업데이트할 수 있는 함수
    const updateGlobalUnreadCount = useCallback(async () => {
        if (!currentUser) return;
        try {
            const response = await fetch('/api/chats/my-rooms');
            if(response.ok) {
                const rooms = await response.json();
                setGlobalUnreadCount(calculateTotalUnreadCount(rooms));
            }
        } catch (error) {
            console.error("안 읽은 메시지 개수 업데이트 실패:", error);
        }
    }, [currentUser]);


    const value = {
        notifications,
        globalUnreadCount,
        latestMessageNotification,
        clearLatestNotification,
        updateGlobalUnreadCount,
        stompClient, // Chat.js에서 메시지 전송을 위해 클라이언트 직접 전달
        isConnected
    };

    return (
        <NotificationContext.Provider value={value}>
            {children}
        </NotificationContext.Provider>
    );
};
