import React, { createContext, useState, useContext, useCallback } from 'react';

// 알림 Context 생성
const NotificationContext = createContext(null);

// 알림 Context를 쉽게 사용할 수 있는 커스텀 훅
export const useNotifications = () => useContext(NotificationContext);

// 알림 Provider 컴포넌트
export const NotificationProvider = ({ children }) => {
    // 최신 메시지 알림 상태 (null이거나 { roomId, sender, content, timestamp } 객체)
    const [latestMessageNotification, setLatestMessageNotification] = useState(null);
    // 전역 읽지 않은 메시지 개수
    const [globalUnreadCount, setGlobalUnreadCount] = useState(0);

    // 최신 알림을 지우는 함수
    const clearLatestNotification = useCallback(() => {
        setLatestMessageNotification(null);
    }, []);

    // 전역 읽지 않은 메시지 개수를 1 증가시키는 함수
    const incrementGlobalUnreadCount = useCallback(() => {
        setGlobalUnreadCount(prev => prev + 1);
    }, []);

    // 전역 읽지 않은 메시지 개수를 0으로 초기화하는 함수
    const resetGlobalUnreadCount = useCallback(() => {
        setGlobalUnreadCount(0);
    }, []);

    // Context를 통해 제공할 값들
    const value = {
        latestMessageNotification,
        setLatestMessageNotification, // useStompChat에서 호출될 수 있도록 노출
        globalUnreadCount,
        incrementGlobalUnreadCount, // Chat.js나 다른 곳에서 호출될 수 있도록 노출
        resetGlobalUnreadCount,   // Chat.js에서 호출될 수 있도록 노출
        clearLatestNotification,
    };

    return (
        <NotificationContext.Provider value={value}>
            {children}
        </NotificationContext.Provider>
    );
};
