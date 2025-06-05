import React, {useState, useRef, useEffect, useCallback} from 'react';
import { Client } from '@stomp/stompjs';
import { useNavigate } from "react-router-dom";
import './Chat.css'; // CSS 클래스 버전을 사용한다고 가정

// SVG 아이콘 컴포넌트들 (이전과 동일, className props 사용)
const Send = ({ className }) => (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
    </svg>
);

const Smile = ({ className }) => (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="10"/>
        <path d="m9 9 6 0"/>
        <path d="m9 15 6 0"/>
    </svg>
);

const MoreVertical = ({ className }) => (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="1"/>
        <circle cx="12" cy="5" r="1"/>
        <circle cx="12" cy="19" r="1"/>
    </svg>
);

const Hash = ({ className }) => (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <line x1="4" y1="9" x2="20" y2="9"/>
        <line x1="4" y1="15" x2="20" y2="15"/>
        <line x1="10" y1="3" x2="8" y2="21"/>
        <line x1="16" y1="3" x2="14" y2="21"/>
    </svg>
);

const Users = ({ className }) => (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
        <circle cx="9" cy="7" r="4"/>
        <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
        <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
    </svg>
);

const ChevronLeft = ({ className }) => (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
    </svg>
);

const ChevronRight = ({ className }) => (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
    </svg>
);

const PRIVATE_ROOM_MAX_ID_FROM_SERVER = 10;

const Chat = () => {
    const [messages, setMessages] = useState({});
    const [newMessage, setNewMessage] = useState('');
    const [isConnected, setIsConnected] = useState(false);
    const stompClientRef = useRef(null); // STOMP 클라이언트 인스턴스

    const [chatRooms, setChatRooms] = useState([]);
    const [currentChatRoom, setCurrentChatRoom] = useState(null);
    const [roomParticipants, setRoomParticipants] = useState({});

    const [chatRoomsLoading, setChatRoomsLoading] = useState(true);
    const [chatRoomsError, setChatRoomsError] = useState('');
    const [participantsLoading, setParticipantsLoading] = useState(false);

    // 전체 사용자 목록 관련
    const [allUsers, setAllUsers] = useState([]);
    const [allUsersLoading, setAllUsersLoading] = useState(false);
    const [allUsersError, setAllUsersError] = useState('');

    const [isRightSidebarOpen, setIsRightSidebarOpen] = useState(true);
    const [currentUser, setCurrentUser] = useState(null);
    const [userFetchError, setUserFetchError] = useState('');
    const navigate = useNavigate();

    const messagesEndRef = useRef(null);

    // 웹소켓 연결 확인 (Connected Users) 상태
    const [connectedUsers, setConnectedUsers] = useState(new Set());


    // 스크롤 핸들러
    const scrollToBottom = useCallback(() => {
        messagesEndRef.current?.scrollIntoView({behavior: "smooth"});
    }, []);

    useEffect(() => {
        scrollToBottom();
    }, [messages, currentChatRoom, scrollToBottom]);

    // 현재 사용자 정보 가져오기
    useEffect(() => {
        const fetchCurrentUser = async () => {
            try {
                const response = await fetch('/api/users/me');
                if (response.ok) {
                    const userData = await response.json();
                    if (response.redirected && response.url.includes('/login')) {
                        navigate('/login');
                        return;
                    }
                    console.log(`Fetched current user: ${userData.username} (ID: ${userData.id})`);
                    setCurrentUser({
                        id: userData.id,
                        name: userData.username, // userData.username을 name으로 설정
                        avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=40&h=40&fit=crop&crop=face'
                    });
                    setUserFetchError('');
                } else if (response.status === 401) {
                    setUserFetchError('로그인이 필요합니다. 로그인 페이지로 이동합니다.');
                    navigate('/login');
                } else {
                    const errorText = await response.text();
                    setUserFetchError(`사용자 정보 로딩 실패 ${response.status}: ${errorText}`);
                }
            } catch (error) {
                setUserFetchError('네트워크 오류로 사용자 정보를 가져올 수 없습니다.');
                console.error('Network error fetching current user:', error);
            }
        };
        fetchCurrentUser();
    }, [navigate]);

    // 채팅방 목록 조회
    useEffect(() => {
        if (currentUser && currentUser.id) {
            const fetchChatRooms = async () => {
                setChatRoomsLoading(true);
                setChatRoomsError('');
                try {
                    const response = await fetch(`/api/chats/my-rooms`);
                    if (response.ok) {
                        const roomsData = await response.json();
                        setChatRooms(roomsData || []);
                        if (roomsData && roomsData.length > 0) {
                            const currentRoomExists = roomsData.some(room => room.id === currentChatRoom);
                            if (!currentChatRoom || !currentRoomExists) {
                                setCurrentChatRoom(roomsData[0].id);
                            }
                        } else {
                            setCurrentChatRoom(null); // 채팅방이 없으면 현재 선택된 방을 null로 설정
                        }
                    } else {
                        console.error("Failed to fetch chat rooms:", response.status);
                        setChatRoomsError(`채팅방 목록 로딩 실패 (${response.status})`);
                    }
                } catch (error) {
                    console.error("Error fetching chat rooms:", error);
                    setChatRoomsError('네트워크 오류로 채팅방 목록을 가져올 수 없습니다.');
                } finally {
                    setChatRoomsLoading(false);
                }
            };
            fetchChatRooms();
        }
    }, [currentUser, currentChatRoom]);

    // 전체 사용자 목록 조회
    useEffect(() => {
        if (currentUser && currentUser.id) {
            const fetchAllUsers = async () => {
                setAllUsersLoading(true);
                setAllUsersError('');
                try {
                    // 올바른 API 경로: /api/users
                    const response = await fetch(`/api/users`);
                    if (response.ok) {
                        const usersData = await response.json();
                        const formattedUsers = usersData.map(u => ({
                            ...u,
                            avatar: 'https://via.placeholder.com/32/CCCCCC/FFFFFF/?text=' + u.username.charAt(0).toUpperCase()
                        }));
                        setAllUsers(formattedUsers);
                        console.log("Fetched all users:", formattedUsers);
                    } else {
                        const errorText = await response.text();
                        console.error("Failed to fetch all users:", response.status, errorText);
                        setAllUsersError(`전체 사용자 목록 로딩 실패 (${response.status}): ${errorText}`);
                    }
                } catch (error) {
                    console.error("Error fetching all users:", error);
                    setAllUsersError('네트워크 오류로 전체 사용자 목록을 가져올 수 없습니다.');
                } finally {
                    setAllUsersLoading(false);
                }
            };
            fetchAllUsers();
        }
    }, [currentUser]);

    // 특정 채팅방 참여자 목록 조회 (기존 로직 유지)
    useEffect(() => {
        if (currentChatRoom && currentUser && currentUser.id) {
            const fetchRoomParticipants = async () => {
                setParticipantsLoading(true);
                try {
                    const response = await fetch(`/api/chat/rooms/${currentChatRoom}/participants`);
                    if (response.ok) {
                        const participantsData = await response.json();
                        setRoomParticipants(prev => ({
                            ...prev,
                            [currentChatRoom]: participantsData || []
                        }));
                    } else {
                        console.error(`Failed to fetch participants for room ${currentChatRoom}:`, response.status);
                    }
                } catch (error) {
                    console.error(`Error fetching participants for room ${currentChatRoom}:`, error);
                } finally {
                    setParticipantsLoading(false);
                }
            };
            fetchRoomParticipants();
        }
    }, [currentChatRoom, currentUser]);


    // [수정된 부분 1]: 웹소켓 기본 연결 및 connectedUsers 구독 (currentUser만 있으면 작동)
    useEffect(() => {
        // currentUser 정보가 없으면 연결 시도 안 함
        if (!currentUser || !currentUser.id) {
            // 기존 연결이 있다면 비활성화
            if (stompClientRef.current && stompClientRef.current.active) {
                console.log("STOMP (Basic): Deactivating existing client due to missing user info.");
                stompClientRef.current.deactivate();
                stompClientRef.current = null; // 클라이언트 비활성화 시 참조 제거
            }
            setIsConnected(false);
            return;
        }

        // 이미 연결되어 있고, 사용자 변경이 없으면 재연결 불필요 (userIdBeforeDeactivation로 사용자 변경 감지)
        // 이 useEffect는 기본 연결과 connectedUsers 구독을 위한 것이므로, 룸 변경에 따른 재연결은 여기서 관리하지 않음.
        if (stompClientRef.current && stompClientRef.current.active &&
            stompClientRef.current.userIdBeforeDeactivation === currentUser.id) {
            console.log("STOMP (Basic): Already connected for connectedUsers, user unchanged.");
            if (!isConnected) setIsConnected(true);
            return;
        }

        console.log(`STOMP (Basic): Attempting to connect for connectedUsers as user: ${currentUser.id}`);

        const wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
        const wsHost = window.location.hostname;
        const wsPort = process.env.REACT_APP_WS_PORT || '9443'; // .env 설정 또는 기본 9443
        const wsEndpoint = process.env.REACT_APP_WS_ENDPOINT || '/ws';
        const brokerURL = `${wsProtocol}://${wsHost}:${wsPort}${wsEndpoint}`;

        const client = new Client({
            brokerURL,
            connectHeaders: {
                username: currentUser.name, // 백엔드 WebSocketConfig와 일치하도록 username 사용
            },
            debug: (str) => {
                console.log(new Date(), 'STOMP DEBUG (Basic): ', str);
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            onConnect: (frame) => {
                setIsConnected(true);
                stompClientRef.current = client; // Ref 업데이트
                stompClientRef.current.userIdBeforeDeactivation = currentUser.id; // 사용자 정보 저장
                console.log(`STOMP (Basic) Connected as user: ${currentUser.id}`, frame);

                // connectedUsers 토픽 구독 (연결 시마다 구독)
                client.subscribe('/topic/connectedUsers', (message) => {
                    try {
                        const updatedConnectedUsersArray = JSON.parse(message.body);
                        setConnectedUsers(new Set(updatedConnectedUsersArray));
                        console.log("Updated connected users via WebSocket: ", updatedConnectedUsersArray);
                    } catch (error) {
                        console.error("Failed to parse connected users update message: ", message.body, error);
                    }
                });
                console.log("Subscribed to /topic/connectedUsers for real-time updates.");
            },
            onStompError: (frame) => {
                console.error('STOMP (Basic): Broker reported error: ' + frame.headers['message'], frame.body);
                setIsConnected(false);
            },
            onWebSocketClose: (event) => {
                console.log('STOMP (Basic): WebSocket closed.', event);
                setIsConnected(false);
            },
            onDisconnect: (frame) => {
                console.log('STOMP (Basic): Client disconnected.', frame);
                setIsConnected(false);
            }
        });

        client.activate();

        // Cleanup 함수: 컴포넌트 언마운트 시 또는 currentUser가 변경되어 이 useEffect가 재실행될 때 기존 클라이언트 비활성화
        return () => {
            if (stompClientRef.current && stompClientRef.current.active) {
                console.log(`Deactivating STOMP client (Basic) for user: ${currentUser.id} during cleanup.`);
                stompClientRef.current.deactivate();
                stompClientRef.current = null; // 참조 초기화
            }
        };
    }, [currentUser]); // currentUser가 변경될 때만 이 useEffect를 재실행

    // [수정된 부분 2]: 채팅방 메시지 구독 (currentChatRoom이 있을 때만)
    useEffect(() => {
        // currentChatRoom, currentUser, stompClient가 활성화되어 있지 않으면 구독하지 않음
        if (!currentUser || !currentUser.id || !currentChatRoom || !stompClientRef.current || !stompClientRef.current.active) {
            // 이전 구독이 있다면 여기서 해제될 수 있도록 return
            return;
        }

        const selectedRoomObject = chatRooms.find(room => room.id === currentChatRoom);
        if (!selectedRoomObject) {
            console.warn(`STOMP: Room data not found in local state for currentChatRoom: ${currentChatRoom}. Message subscription deferred.`);
            return;
        }

        const intRoomIdForSubscription = selectedRoomObject.intId;
        const subscriptionDestination = intRoomIdForSubscription <= PRIVATE_ROOM_MAX_ID_FROM_SERVER
            ? `/topic/private/${intRoomIdForSubscription}`
            : `/topic/group/${intRoomIdForSubscription}`;

        console.log(`STOMP: Subscribing to messages for room ${currentChatRoom} at ${subscriptionDestination}`);

        // [중요] 기존 구독 해제: 방이 바뀌거나 언마운트될 때 기존 구독을 해제해야 중복 메시지를 받지 않음
        // stompClientRef.current는 이미 첫 번째 useEffect에서 활성화된 상태여야 함.
        const subscription = stompClientRef.current.subscribe(subscriptionDestination, (message) => {
            try {
                const receivedMsg = JSON.parse(message.body);
                console.log(`Message received for room ${currentChatRoom} (subscribed to ${subscriptionDestination}):`, receivedMsg);
                const messageToStore = {
                    id: receivedMsg.messageId || receivedMsg.id || Date.now(),
                    sender: receivedMsg.username || 'Unknown',
                    senderId: receivedMsg.senderId,
                    content: receivedMsg.content,
                    time: receivedMsg.timestamp ? new Date(receivedMsg.timestamp).toLocaleTimeString('ko-KR', {
                        hour: 'numeric',
                        minute: '2-digit',
                        hour12: true
                    }) : new Date().toLocaleTimeString('ko-KR', {
                        hour: 'numeric',
                        minute: '2-digit',
                        hour12: true
                    }),
                    isOwn: receivedMsg.senderId === currentUser.id.toString(),
                    avatar: receivedMsg.avatar || (receivedMsg.senderId === currentUser.id.toString() ? currentUser.avatar : 'https://via.placeholder.com/40'),
                };

                setMessages(prevMessages => {
                    const currentRoomMessages = prevMessages[currentChatRoom] || [];
                    if (currentRoomMessages.find(msg => msg.id === messageToStore.id)) {
                        return prevMessages;
                    }
                    return {
                        ...prevMessages,
                        [currentChatRoom]: [...currentRoomMessages, messageToStore]
                    };
                });
            } catch (error) {
                console.error("Failed to parse STOMP message:", message.body, error);
            }
        });

        // Cleanup 함수: 현재 채팅방이 변경되거나 컴포넌트 언마운트 시 기존 구독 해제
        return () => {
            if (stompClientRef.current && stompClientRef.current.active) {
                console.log(`Unsubscribing from ${subscriptionDestination}`);
                subscription.unsubscribe(); // 특정 구독만 해제
            }
        };
    }, [currentChatRoom, currentUser, chatRooms, isConnected]); // isConnected도 의존성 추가


    const handleSendMessage = () => {
        const selectedRoom = chatRooms.find(room => room.id === currentChatRoom);
        if (!selectedRoom) {
            console.error("Cannot send message, selected room not found in local state:", currentChatRoom);
            alert("선택된 채팅방 정보를 찾을 수 없습니다.");
            return;
        }

        const integerRoomId = selectedRoom.intId;
        if (newMessage.trim() && currentUser && stompClientRef.current && stompClientRef.current.active) {
            const messagePayload = {
                senderId: currentUser.id.toString(),
                roomId: integerRoomId,
                username: currentUser.name,
                content: newMessage.trim(),
                timestamp: new Date().toISOString(),
            };

            stompClientRef.current.publish({
                destination: `/app/chat.sendMessage/${currentChatRoom}`,
                body: JSON.stringify(messagePayload)
            });

            setNewMessage('');
        } else {
            if (!currentUser) {
                alert('사용자 정보가 로드되지 않았습니다.');
            } else if (!stompClientRef.current || !stompClientRef.current.active) {
                alert('채팅 서버에 연결되어 있지 않습니다.');
            }
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    const handleRoomChange = (roomId) => {
        if (roomId !== currentChatRoom) {
            setMessages(prev => ({...prev, [roomId]: prev[roomId] || []}));
            setCurrentChatRoom(roomId);
        }
    };

    const getCurrentRoom = () => chatRooms.find(room => room.id === currentChatRoom);

    const getCurrentMessages = () => messages[currentChatRoom] || [];

    // 현재 채팅방 참여자 목록 (기존 로직 유지)
    const getCurrentParticipants = () => {
        const currentRoomKey = currentChatRoom;
        const participants = currentRoomKey ? (roomParticipants[currentRoomKey] || []) : [];

        if (currentUser) {
            return participants.map(p =>
                p.id === currentUser.id.toString() ? {...p, avatar: currentUser.avatar, name: currentUser.name} : p
            );
        }
        return participants;
    };

    // [수정된 부분 3]: 모든 사용자 목록 + 온라인 상태를 포함하여 사이드바에 표시할 함수
    const getDisplayedUsersInRightSidebar = useCallback(() => {
        if (!allUsers || allUsers.length === 0) {
            return [];
        }

        const usersWithStatus = allUsers.map(u => ({
            ...u,
            // connectedUsers Set에는 username이 저장되어 있으므로 u.username으로 확인
            status: connectedUsers.has(u.username) ? 'online' : 'offline',
            avatar: u.avatar || 'https://via.placeholder.com/32/CCCCCC/FFFFFF/?text=' + u.username.charAt(0).toUpperCase()
        }));

        let sortedUsers = [];
        if (currentUser) {
            // currentUser의 id와 비교할 때는 여전히 id를 사용합니다.
            const otherUsers = usersWithStatus.filter(u => u.id !== currentUser.id.toString());
            const me = usersWithStatus.find(u => u.id === currentUser.id.toString());

            if (me) {
                sortedUsers.push({
                    ...me,
                    avatar: currentUser.avatar,
                    // currentUser 객체의 name (username) 사용
                    name: currentUser.name, // <-- user.username 대신 currentUser.name으로 수정
                    status: connectedUsers.has(currentUser.name) ? 'online' : 'offline' // <-- currentUser.username 대신 currentUser.name
                });
            }
            sortedUsers = sortedUsers.concat(otherUsers);
        } else {
            sortedUsers = usersWithStatus;
        }

        // 온라인 사용자를 먼저, 그 다음 이름순으로 정렬
        sortedUsers.sort((a, b) => {
            if (a.status === 'online' && b.status !== 'online') return -1;
            if (a.status !== 'online' && b.status === 'online') return 1;
            // [수정된 부분 4]: user.name 대신 user.username으로 정렬
            return a.username.localeCompare(b.username);
        });

        return sortedUsers;
    }, [allUsers, connectedUsers, currentUser]);


    return (
        <div className="chat-container">
            {/* 왼쪽 사이드바 - 채팅방 목록 */}
            <div className="left-sidebar">
                <div className="sidebar-header">
                    <h2 className="sidebar-title">프로젝트 채팅</h2>
                    <div className="connection-status">
                        <div className={`status-indicator ${isConnected ? 'connected' : 'disconnected'}`}></div>
                        <span className="status-text">
                            {isConnected ? '연결됨' : '연결 중...'}
                        </span>
                    </div>
                </div>

                <div className="rooms-list">
                    {/* 로딩 중일 때 */}
                    {chatRoomsLoading && <div className="loading-text">채팅방 로딩 중...</div>}

                    {/* 에러 발생 시 */}
                    {chatRoomsError && <div className="error-text">오류: {chatRoomsError}</div>}

                    {/* 로딩 완료 후 채팅방이 없을 때 */}
                    {!chatRoomsLoading && !chatRoomsError && chatRooms.length === 0 && (
                        <div className="empty-rooms-message">
                            <p>참여 중인 채팅방이 없습니다.</p>
                            <p>새로운 채팅방을 생성하거나 프로젝트에 참여하여 채팅을 시작하세요.</p>
                        </div>
                    )}

                    {/* 채팅방 목록이 있을 때만 맵핑하여 렌더링 */}
                    {!chatRoomsLoading && !chatRoomsError && chatRooms.length > 0 && chatRooms.map((room) => (
                        <div
                            key={room.id}
                            onClick={() => handleRoomChange(room.id)}
                            className={`room-item ${currentChatRoom === room.id ? 'active' : ''}`}
                        >
                            <div className="room-content">
                                <div className="room-avatar" style={{backgroundColor: room.color}}>
                                    <Hash className="room-icon"/>
                                </div>
                                <div className="room-info">
                                    <div className="room-header">
                                        <h3 className="room-name">{room.name}</h3>
                                        {room.unreadCount > 0 && (
                                            <span className="unread-badge">
                                {room.unreadCount}
                            </span>
                                        )}
                                    </div>
                                    <p className="room-description">{room.description}</p>
                                    <p className="room-last-message">{room.lastMessage}</p>
                                    <div className="room-footer">
                                        <span className="last-message-time">{room.lastMessageTime}</span>
                                        <div className="participants-count">
                                            <Users className="participants-icon"/>
                                            {room.participants}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* 메인 채팅 영역 */}
            <div className="main-chat">
                {/* 채팅 헤더 */}
                <div className="chat-header">
                    <div className="header-left">
                        <div className="current-room-avatar" style={{backgroundColor: getCurrentRoom()?.color}}>
                            <Hash className="room-icon"/>
                        </div>
                        <div className="current-room-info">
                            <h3 className="current-room-name">{getCurrentRoom()?.name}</h3>
                            <p className="current-room-participants">{getCurrentParticipants().length}명 참여 중</p>
                        </div>
                    </div>

                    <div className="header-actions">
                        <button
                            onClick={() => setIsRightSidebarOpen(!isRightSidebarOpen)}
                            className="action-button"
                            title={isRightSidebarOpen ? "참여자 목록 숨기기" : "참여자 목록 보기"}
                        >
                            <Users className="action-icon"/>
                        </button>
                        <button className="action-button">
                            <MoreVertical className="action-icon"/>
                        </button>
                    </div>
                </div>

                {/* 메시지 영역 */}
                <div className="messages-container">
                    {getCurrentMessages().length === 0 ? (
                        // 빈 채팅방 상태
                        <div className="empty-chat">
                            <div className="empty-chat-avatar" style={{backgroundColor: getCurrentRoom()?.color}}>
                                <Hash className="empty-chat-icon"/>
                            </div>
                            <h3 className="empty-chat-title">{getCurrentRoom()?.name}</h3>
                            <p className="empty-chat-description">
                                {getCurrentRoom()?.description}<br/>
                                팀원들과 소통을 시작해보세요.
                            </p>
                        </div>
                    ) : (
                        // 메시지 목록
                        <div className="messages-list">
                            {getCurrentMessages().map((message) => (
                                <div key={message.id} className={`message ${message.isOwn ? 'own' : 'other'}`}>
                                    <div className="message-content">
                                        <img
                                            src={message.avatar}
                                            alt={message.sender}
                                            className="message-avatar"
                                        />
                                        <div className="message-body">
                                            {!message.isOwn && (
                                                <span className="message-sender">{message.sender}</span>
                                            )}
                                            <div className={`message-bubble ${message.isOwn ? 'own-bubble' : 'other-bubble'}`}>
                                                <p className="message-text">{message.content}</p>
                                            </div>
                                            <span className="message-time">{message.time}</span>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                    <div ref={messagesEndRef}/>
                </div>

                {/* 메시지 입력 영역 */}
                <div className="message-input-container">
                    <div className="message-input-wrapper">
                        <textarea
                            value={newMessage}
                            onChange={(e) => setNewMessage(e.target.value)}
                            onKeyPress={handleKeyPress}
                            //currentChatRoom이 null이면 플레이스홀더변걍
                            placeholder={currentUser ? `${getCurrentRoom()?.name}}에 메시지 보내기...` : "메시지를 보낼 채팅방 선택하거나 생성하세요..."}
                            className="message-input"
                            rows="1"
                            //currentChatRoom이 null이면 비활성화
                            disabled={!currentChatRoom}
                        />

                        <button className="input-action-button">
                            <Smile className="input-action-icon"/>
                        </button>

                        <button
                            onClick={handleSendMessage}
                            disabled={!newMessage.trim()}
                            className={`send-button ${!newMessage.trim() ? 'disabled' : 'enabled'}`}
                        >
                            <Send className="send-icon"/>
                        </button>
                    </div>
                </div>
            </div>

            {/* 오른쪽 사이드 바 - 모든 사용자 목록 (온라인 상태 포함)*/}
            {isRightSidebarOpen && (
                <div className="right-sidebar">
                    <div className="participants-header">
                        <div className="participants-info">
                            {/* 텍스트 변경: "참여자" -> "모든 사용자" 또는 "사용자" */}
                            <h3 className="participants-title">모든 사용자</h3>
                            {/* 현재 사이드바에 표시되는 사용자 수를 getDisplayedUsersInRightSidebar().length로 표시 */}
                            <p className="participants-count">{getDisplayedUsersInRightSidebar().length}명</p>
                        </div>
                        <button
                            onClick={() => setIsRightSidebarOpen(false)}
                            className="close-sidebar-button"
                            title="사용자 목록 닫기" // 툴팁 텍스트 변경
                        >
                            <ChevronRight className="close-sidebar-icon"/>
                        </button>
                    </div>

                    <div className="participants-list">
                        <div className="participants-grid">
                            {/* getCurrentParticipants() 대신 getDisplayedUsersInRightSidebar() 사용 */}
                            {getDisplayedUsersInRightSidebar().map((user) => (
                                <div key={user.id} className="participant-item">
                                    <div className="participant-avatar-container">
                                        <img src={user.avatar} alt={user.username} className="participant-avatar"/>
                                        <div className={`participant-status ${user.status === 'online' ? 'online' : 'offline'}`}></div>
                                    </div>
                                    <div className="participant-info">
                                        <p className="participant-name">{user.username}</p> {/* user.name 대신 user.username 사용 */}
                                        <p className="participant-status-text">
                                            {/* user.status는 'online' 또는 'offline'이므로 조건 변경 */}
                                            {user.status === 'online' ? '온라인' : '오프라인'}
                                        </p>
                                    </div>
                                </div>
                            ))}
                            {/* 로딩 및 에러 메시지 추가 (getDisplayedUsersInRightSidebar 함수에 사용된 allUsersLoading/allUsersError 상태 참조) */}
                            {allUsersLoading && <div className="loading-text">사용자 목록 로딩 중...</div>}
                            {allUsersError && <div className="error-text">오류: {allUsersError}</div>}
                        </div>
                    </div>
                </div>
            )}

            {/* 사이드바가 닫혔을 때 토글 버튼 */}
            {!isRightSidebarOpen && (
                <div className="sidebar-toggle">
                    <button
                        onClick={() => setIsRightSidebarOpen(true)}
                        className="toggle-button"
                        title="사용자 목록 열기" // 툴팁 텍스트 변경
                    >
                        <ChevronLeft className="toggle-icon"/>
                    </button>
                    <div className="toggle-text">사용자</div> {/* 텍스트 변경 */}
                </div>
            )}
        </div>
    );
};

export default Chat;