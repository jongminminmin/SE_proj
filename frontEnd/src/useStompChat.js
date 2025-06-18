// useStompChat.js
import { useState, useRef, useEffect, useCallback } from 'react';
import { Client } from '@stomp/stompjs';

const PRIVATE_ROOM_MAX_ID_FROM_SERVER = 10;


const useStompChat = (currentUser, currentChatRoom, chatRooms, onMessage, onConnectedUsersUpdate, navigate) => {
    const [isConnected, setIsConnected] = useState(false); // STOMP 연결 상태
    const stompClientRef = useRef(null); // STOMP 클라이언트 인스턴스 참조

    // STOMP 클라이언트 연결 로직
    // currentUser가 변경되거나, 연결 상태가 바뀔 때 실행됩니다.
    useEffect(() => {
        // currentUser 정보가 없으면 연결 시도하지 않음
        if (!currentUser || !currentUser.name) {
            console.log("STOMP (Basic): currentUser 정보가 없어 연결 시도 안 함.");
            if (stompClientRef.current && stompClientRef.current.active) {
                console.log("STOMP (Basic): 기존 클라이언트 비활성화.");
                stompClientRef.current.deactivate();
                stompClientRef.current = null;
            }
            setIsConnected(false);
            return;
        }

        // 이미 연결되어 있고 사용자 변경이 없으면 재활성화 건너뛰기
        if (stompClientRef.current && stompClientRef.current.active &&
            stompClientRef.current.userIdBeforeDeactivation === currentUser.name) {
            console.log("STOMP (Basic): 이미 연결되어 있고 사용자 변경 없음. 재활성화 건너뛰기.");
            if (!isConnected) setIsConnected(true);
            return;
        }

        console.log(`STOMP (Basic): 웹소켓 연결 시도 중 (사용자: ${currentUser.name})`);

        // 웹소켓 연결 URL 구성
        const wsProtocol = 'ws';
        const wsHost = window.location.hostname;
        const wsPort = '9000'; // Spring Boot 웹소켓 포트
        const wsEndpoint = '/ws';
        const brokerURL = `${wsProtocol}://${wsHost}:${wsPort}${wsEndpoint}`;

        // 새 연결 시도 전 기존 클라이언트 비활성화 (클린업)
        if (stompClientRef.current && stompClientRef.current.active) {
            console.log("STOMP (Basic): 새 연결 전 기존 클라이언트 비활성화.");
            stompClientRef.current.deactivate();
            stompClientRef.current = null;
        }

        // STOMP Client 인스턴스 생성 및 설정
        const client = new Client({
            brokerURL,
            connectHeaders: {
                username: currentUser.name, // 연결 시 사용자 이름 전달
            },
            debug: (str) => {
                console.log(new Date(), 'STOMP DEBUG (Basic): ', str); // 디버그 로그
            },
            reconnectDelay: 5000, // 재연결 지연 시간
            heartbeatIncoming: 15000, // 수신 하트비트
            heartbeatOutgoing: 15000, // 발신 하트비트
            onConnect: (frame) => {
                // 연결 성공 시
                setIsConnected(true);
                stompClientRef.current = client; // 클라이언트 인스턴스 저장
                stompClientRef.current.userIdBeforeDeactivation = currentUser.name;
                console.log(`STOMP (Basic) 연결 성공 (사용자: ${currentUser.name})`, frame);

                // /topic/connectedUsers 토픽 구독: 접속 사용자 목록 업데이트
                client.subscribe('/topic/connectedUsers', (message) => {
                    try {
                        const updatedConnectedUsersArray = JSON.parse(message.body);
                        onConnectedUsersUpdate(updatedConnectedUsersArray); // 콜백 호출
                        console.log("웹소켓을 통해 연결된 사용자 목록 업데이트됨: ", updatedConnectedUsersArray);
                    } catch (error) {
                        console.error("연결된 사용자 업데이트 메시지 파싱 실패: ", message.body, error);
                    }
                });
                console.log("/topic/connectedUsers 토픽 구독됨.");
            },
            onStompError: (frame) => {
                // STOMP 오류 발생 시
                console.error('STOMP (Basic): 브로커 오류 발생: ' + frame.headers['message'], frame.body);
                setIsConnected(false);
            },
            onWebSocketClose: (event) => {
                // 웹소켓 연결이 닫힐 때
                console.log('STOMP (Basic): 웹소켓 닫힘.', event);
                setIsConnected(false);
            },
            onDisconnect: (frame) => {
                // 클라이언트 연결 해제 시
                console.log('STOMP (Basic): 클라이언트 연결 해제됨.', frame);
                setIsConnected(false);
            }
        });

        client.activate(); // STOMP 클라이언트 활성화 (연결 시작)

        // useEffect 클린업 함수: 컴포넌트 언마운트 또는 종속성 변경 시 호출
        return () => {
            if (stompClientRef.current && stompClientRef.current.active) {
                console.log(`STOMP 클라이언트 (Basic) 정리 중 비활성화 (사용자: ${currentUser.name}).`);
                stompClientRef.current.deactivate(); // 클라이언트 비활성화
                stompClientRef.current = null;
                setIsConnected(false);
            }
        };
    }, [currentUser, navigate, onConnectedUsersUpdate]); // 종속성 배열

    // 채팅방 메시지 구독 로직
    // currentChatRoom, currentUser, chatRooms, isConnected, onMessage 변경 시 실행
    useEffect(() => {
        // 필수 조건 미충족 시 구독 시도하지 않음
        if (!currentUser?.id || !currentChatRoom || !stompClientRef.current?.active) {
            console.log("STOMP (Messages): 메시지 구독을 위한 필수 조건 미충족.");
            return;
        }

        // 현재 채팅방 객체 찾기
        const selectedRoomObject = chatRooms.find(room => room.id === currentChatRoom);
        if (!selectedRoomObject) {
            console.warn(`STOMP (Messages): currentChatRoom (${currentChatRoom})에 대한 방 데이터가 로컬 상태에서 발견되지 않음. 메시지 구독 연기.`);
            return;
        }

        // 구독할 토픽 경로 결정 (개인 채팅방 또는 그룹 채팅방)
        const intRoomIdForSubscription = selectedRoomObject.intId;
        const subscriptionDestination = intRoomIdForSubscription <= PRIVATE_ROOM_MAX_ID_FROM_SERVER
            ? `/topic/private/${intRoomIdForSubscription}`
            : `/topic/group/${intRoomIdForSubscription}`;

        console.log(`STOMP (Messages): 방 ${currentChatRoom}의 메시지 구독 시도 중 (${subscriptionDestination})`);

        // 기존 구독이 있다면 해제하여 중복 구독 방지
        let messageSubscription = null;
        if (stompClientRef.current.subscriptions && Object.keys(stompClientRef.current.subscriptions).length > 0) {
            for (const subId in stompClientRef.current.subscriptions) {
                if (stompClientRef.current.subscriptions[subId].subscribeHeaders.destination === subscriptionDestination) {
                    console.log(`기존 구독 ${subId} (${subscriptionDestination}) 해제 중.`);
                    stompClientRef.current.subscriptions[subId].unsubscribe();
                    break;
                }
            }
        }

        // 새 메시지 토픽 구독
        messageSubscription = stompClientRef.current.subscribe(subscriptionDestination, (message) => {
            try {
                const receivedMsg = JSON.parse(message.body);
                // onMessage 콜백으로 메시지와 현재 방에 대한 것인지 여부를 전달
                onMessage(receivedMsg, receivedMsg.chatRoomId === currentChatRoom);
            } catch (error) {
                console.error("STOMP 메시지 파싱 실패:", message.body, error);
            }
        });

        // useEffect 클린업 함수
        return () => {
            if (stompClientRef.current && stompClientRef.current.active && messageSubscription) {
                console.log(`${subscriptionDestination} 구독 해제 중.`);
                messageSubscription.unsubscribe();
            }
        };
    }, [currentChatRoom, currentUser, chatRooms, isConnected, onMessage]); // 종속성 배열

    // 읽음 상태 구독 로직 (현재는 콜백을 통해 처리하지 않고, Chat.js에서 직접 처리)
    useEffect(() => {
        if (!currentChatRoom || !stompClientRef.current?.active || !currentUser?.id) return;

        let readStatusSubscription = null;
        const readStatusDestination = `/topic/readStatus/${currentChatRoom}`;

        if (stompClientRef.current.subscriptions) {
            for (const subId in stompClientRef.current.subscriptions) {
                if (stompClientRef.current.subscriptions[subId].subscribeHeaders.destination === readStatusDestination) {
                    console.log(`기존 읽음 상태 구독 ${subId} (${readStatusDestination}) 해제 중.`);
                    stompClientRef.current.subscriptions[subId].unsubscribe();
                    break;
                }
            }
        }

        // 읽음 상태 토픽 구독 (이 메시지는 Chat.js에서 직접 처리)
        readStatusSubscription = stompClientRef.current.subscribe(
            readStatusDestination,
            (message) => {
                try {
                    const readData = JSON.parse(message.body);
                    // 이 로직은 Chat.js에서 직접 처리하거나, 필요하다면 추가 콜백을 통해 전달할 수 있습니다.
                    // 현재는 글로벌 알림과는 직접적인 관련이 없어 useStompChat 외부에서 처리합니다.
                } catch (error) {
                    console.error("읽음 상태 업데이트 실패:", error);
                }
            }
        );

        return () => {
            if (readStatusSubscription) {
                readStatusSubscription.unsubscribe();
            }
        };
    }, [currentChatRoom, currentUser]);


    // 메시지 전송 함수 (useCallback으로 최적화)
    const sendMessage = useCallback((chatRoomId, messagePayload) => {
        if (stompClientRef.current && stompClientRef.current.active) {
            stompClientRef.current.publish({
                destination: `/app/chat.sendMessage/${chatRoomId}`,
                body: JSON.stringify(messagePayload)
            });
            console.log(`메시지 전송: ${messagePayload.content} to room ${chatRoomId}`);
        } else {
            console.warn('채팅 서버에 연결되어 있지 않아 메시지를 보낼 수 없습니다.');
        }
    }, []);

    // 읽음 상태 전송 함수 (useCallback으로 최적화)
    const publishReadStatus = useCallback((roomId, userId, username) => {
        if (stompClientRef.current?.active) {
            stompClientRef.current.publish({
                destination: `/app/chat.markAsRead/${roomId}`,
                body: JSON.stringify({
                    userId: userId,
                    roomId: roomId,
                    timestamp: new Date().toISOString(),
                    username: username
                })
            });
            console.log(`읽음 상태 전송: Room ${roomId}, User ${username}`);
        }
    }, []);

    // 훅에서 반환할 값들
    return {
        isConnected, // STOMP 연결 상태
        sendMessage, // 메시지 전송 함수
        publishReadStatus, // 읽음 상태 전송 함수
    };
};

export default useStompChat;
