// GlobalNotificationDisplay.js
import React, { useEffect, useState } from 'react';
import { useNotifications } from './NotificationContext'; // NotificationContext 훅 임포트
import { useNavigate } from 'react-router-dom';
import './GlobalNotificationDisplay.css'; // 전역 알림 스타일 시트 임포트

const GlobalNotificationDisplay = () => {
    // NotificationContext에서 최신 알림 메시지, 전역 읽지 않은 메시지 개수, 알림 지우기 함수 가져오기
    const { latestMessageNotification, globalUnreadCount, clearLatestNotification } = useNotifications();
    const [showToast, setShowToast] = useState(false); // 토스트 알림 표시 여부
    const navigate = useNavigate(); // 라우팅을 위한 navigate 훅

    // latestMessageNotification이 변경될 때마다 토스트 알림을 표시
    useEffect(() => {
        if (latestMessageNotification) {
            setShowToast(true); // 토스트 표시
            // 5초 후 토스트를 자동으로 숨기고 알림 상태 초기화
            const timer = setTimeout(() => {
                setShowToast(false);
                clearLatestNotification(); // 알림 상태 초기화
            }, 5000); // 5초
            return () => clearTimeout(timer); // 클린업 함수: 타이머 해제
        }
    }, [latestMessageNotification, clearLatestNotification]);

    // 토스트 알림 클릭 시 해당 채팅방으로 이동하는 핸들러
    const handleNotificationClick = () => {
        if (latestMessageNotification) {
            // 알림 클릭 시 해당 채팅방 ID를 쿼리 파라미터로 넘겨 채팅 페이지로 이동
            navigate(`/chat?roomId=${latestMessageNotification.roomId}`);
            clearLatestNotification(); // 알림 클릭 시 알림 상태 초기화
            setShowToast(false); // 토스트 숨기기
        }
    };

    return (
        <>
            {/* 전역 읽지 않은 메시지 개수 뱃지 (선택 사항: UI에 맞게 위치 조정) */}
            {globalUnreadCount > 0 && (
                <div className="global-unread-badge">
                    <span>{globalUnreadCount}</span>
                </div>
            )}

            {/* 토스트 알림 UI */}
            {showToast && latestMessageNotification && (
                <div className="notification-toast" onClick={handleNotificationClick}>
                    <div className="notification-header">
                        새 메시지 - {latestMessageNotification.sender}
                    </div>
                    <div className="notification-body">
                        {latestMessageNotification.content}
                    </div>
                    {/* 닫기 버튼: 클릭 시 토스트를 닫고 알림 상태 초기화 */}
                    <button className="close-toast-button" onClick={(e) => { e.stopPropagation(); setShowToast(false); clearLatestNotification(); }}>X</button>
                </div>
            )}
        </>
    );
};

export default GlobalNotificationDisplay;
