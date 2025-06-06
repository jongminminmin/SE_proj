import React, {useState, useRef, useEffect, useCallback} from 'react';
import { Client } from '@stomp/stompjs';
import { useNavigate } from "react-router-dom";
import './Chat.css';

// SVG 아이콘 컴포넌트들 (className props 사용)
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


// getDisplayedUsersInRightSidebarWithProvidedArray 함수는 컴포넌트 밖으로 분리되어야 합니다.
// Chat 컴포넌트 외부에 정의되어 있어야 합니다.
const getDisplayedUsersInRightSidebarWithProvidedArray = (providedConnectedUsersArray, allUsers, currentUser) => {
    if (!allUsers || allUsers.length === 0) {
        return [];
    }

    const usersWithStatus = allUsers.map(u => {
        const uUsernameLower = u.username ? u.username.toLowerCase() : '';
        const isOnline = providedConnectedUsersArray.some(connectedUsername =>
            connectedUsername && connectedUsername.toLowerCase() === uUsernameLower
        );
        return {
            ...u,
            status: isOnline ? 'online' : 'offline',
            avatar: u.avatar || 'https://via.placeholder.com/32/CCCCCC/FFFFFF/?text=' + u.username.charAt(0).toUpperCase()
        };
    });

    let sortedUsers = [];
    if (currentUser) {
        const currentUserNameLower = currentUser.name ? currentUser.name.toLowerCase() : '';
        const otherUsers = usersWithStatus.filter(u => u.id !== currentUser.id.toString());
        const me = usersWithStatus.find(u => u.id === currentUser.id.toString());

        if (me) {
            const isCurrentUserOnline = providedConnectedUsersArray.some(connectedUsername =>
                connectedUsername && connectedUsername.toLowerCase() === currentUserNameLower
            );
            sortedUsers.push({
                ...me,
                avatar: currentUser.avatar,
                name: currentUser.name,
                status: isCurrentUserOnline ? 'online' : 'offline'
            });
        }
        sortedUsers = sortedUsers.concat(otherUsers);
    } else {
        sortedUsers = usersWithStatus;
    }

    sortedUsers.sort((a, b) => {
        if (a.status === 'online' && b.status !== 'online') return -1;
        if (a.status !== 'online' && b.status === 'online') return 1;
        const aUsername = a.username || '';
        const bUsername = b.username || '';
        return aUsername.localeCompare(bUsername);
    });

    return sortedUsers;
};


const Chat = () => {
    const [messages, setMessages] = useState({});
    const [newMessage, setNewMessage] = useState('');
    const [isConnected, setIsConnected] = useState(false);
    const stompClientRef = useRef(null); // useRef 사용

    const [chatRooms, setChatRooms] = useState([]);
    const [currentChatRoom, setCurrentChatRoom] = useState(null);
    const [roomParticipants, setRoomParticipants] = useState({});

    const [chatRoomsLoading, setChatRoomsLoading] = useState(true);
    const [chatRoomsError, setChatRoomsError] = useState('');
    const [participantsLoading, setParticipantsLoading] = useState(false);

    const [allUsers, setAllUsers] = useState([]);
    const [allUsersLoading, setAllUsersLoading] = useState(false);
    const [allUsersError, setAllUsersError] = useState('');

    const [isRightSidebarOpen, setIsRightSidebarOpen] = useState(true);
    const [currentUser, setCurrentUser] = useState(null);
    const [userFetchError, setUserFetchError] = useState('');
    const navigate = useNavigate();

    const messagesEndRef = useRef(null);

    const [connectedUsers, setConnectedUsers] = useState([]);


    const scrollToBottom = useCallback(() => {
        messagesEndRef.current?.scrollIntoView({behavior: "smooth"});
    }, []);

    useEffect(() => {
        scrollToBottom();
    }, [messages, currentChatRoom, scrollToBottom]);

    useEffect(() => {
        const fetchCurrentUser = async () => {
            try {
                const response = await fetch('/api/users/me');
                if (response.ok) {
                    const userData = await response.json();
                    if (response.redirected && response.url.includes('/login')) {
                        console.log("로그인 페이지로 리디렉션됨. 로그인 페이지로 이동합니다.");
                        navigate('/login');
                        return;
                    }
                    console.log(`현재 사용자 정보 로드 성공: ${userData.username} (ID: ${userData.id})`);
                    setCurrentUser({
                        id: userData.id,
                        name: userData.username,
                        avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=40&h=40&fit=crop&crop=face'
                    });
                    setUserFetchError('');
                } else if (response.status === 401) {
                    setUserFetchError('로그인이 필요합니다. 로그인 페이지로 이동합니다.');
                    navigate('/login');
                } else {
                    const errorText = await response.text();
                    setUserFetchError(`사용자 정보 로딩 실패 (${response.status}): ${errorText}`);
                    console.error(`사용자 정보 로딩 실패 ${response.status}: ${errorText}`);
                }
            } catch (error) {
                setUserFetchError('네트워크 오류로 사용자 정보를 가져올 수 없습니다.');
                console.error('네트워크 오류로 현재 사용자 정보를 가져올 수 없습니다:', error);
            }
        };
        fetchCurrentUser();
    }, [navigate]);

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
                            setCurrentChatRoom(null);
                        }
                    } else {
                        console.error("채팅방 목록 가져오기 실패:", response.status);
                        setChatRoomsError(`채팅방 목록 로딩 실패 (${response.status})`);
                    }
                } catch (error) {
                    console.error("채팅방 목록 가져오는 중 오류 발생:", error);
                    setChatRoomsError('네트워크 오류로 채팅방 목록을 가져올 수 없습니다.');
                } finally {
                    setChatRoomsLoading(false);
                }
            };
            fetchChatRooms();
        }
    }, [currentUser, currentChatRoom]);

    useEffect(() => {
        if (currentUser && currentUser.id) {
            const fetchAllUsers = async () => {
                setAllUsersLoading(true);
                setAllUsersError('');
                try {
                    const response = await fetch(`/api/users`);
                    if (response.ok) {
                        const usersData = await response.json();
                        const formattedUsers = usersData.map(u => ({
                            ...u,
                            avatar: 'https://via.placeholder.com/32/CCCCCC/FFFFFF/?text=' + u.username.charAt(0).toUpperCase()
                        }));
                        setAllUsers(formattedUsers);
                        console.log("전체 사용자 목록 로드 성공:", formattedUsers);
                    } else {
                        const errorText = await response.text();
                        console.error("전체 사용자 목록 가져오기 실패:", response.status, errorText);
                        setAllUsersError(`전체 사용자 목록 로딩 실패 (${response.status}): ${errorText}`);
                    }
                } catch (error) {
                    console.error("전체 사용자 목록 가져오는 중 오류 발생:", error);
                    setAllUsersError('네트워크 오류로 전체 사용자 목록을 가져올 수 없습니다.');
                } finally {
                    setAllUsersLoading(false);
                }
            };
            fetchAllUsers();
        }
    }, [currentUser]);

    // === 추가 부분: 초기 접속자 목록 API 호출 ===
    useEffect(() => {
        if (currentUser && currentUser.id) {
            const fetchInitialConnectedUsers = async () => {
                try {
                    const response = await fetch('/api/users/connected'); // WebSocketUserController의 API 호출
                    if (response.ok) {
                        const connectedUsersData = await response.json();
                        setConnectedUsers(connectedUsersData);
                        console.log("초기 접속자 목록 로드 성공 (API):", connectedUsersData);
                    } else {
                        console.error("초기 접속자 목록 API 호출 실패:", response.status, await response.text());
                    }
                } catch (error) {
                    console.error("초기 접속자 목록 API 호출 중 오류 발생:", error);
                }
            };
            fetchInitialConnectedUsers();
        }
    }, [currentUser]); // currentUser가 로드될 때 호출

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
                        console.error(`채팅방 ${currentChatRoom}의 참여자 목록 가져오기 실패:`, response.status);
                    }
                } catch (error) {
                    console.error(`채팅방 ${currentChatRoom}의 참여자 목록 가져오는 중 오류 발생:`, error);
                } finally {
                    setParticipantsLoading(false);
                }
            };
            fetchRoomParticipants();
        }
    }, [currentChatRoom, currentUser]);


    // [수정 부분] SockJS 없이 순수 WebSocket 연결 시도
    useEffect(() => {
        if (!currentUser || !currentUser.name) {
            console.log("STOMP (Basic): currentUser 정보가 없어 연결 시도 안 함.");
            if (stompClientRef.current && stompClientRef.current.active) {
                console.log("STOMP (Basic): 기존 클라이언트 비활성화.");
                stompClientRef.current.deactivate();
                stompClientRef.current = null;
            }
            setIsConnected(false);
            // setConnectedUsers([]); // 이 부분은 초기 API 호출에서 처리하도록 유지
            return;
        }

        if (stompClientRef.current && stompClientRef.current.active &&
            stompClientRef.current.userIdBeforeDeactivation === currentUser.name) {
            console.log("STOMP (Basic): 이미 연결되어 있고 사용자 변경 없음. 재활성화 건너뛰기.");
            if (!isConnected) setIsConnected(true);
            return;
        }

        console.log(`STOMP (Basic): 웹소켓 연결 시도 중 (사용자: ${currentUser.name})`);

        const wsProtocol = 'ws';
        const wsHost = window.location.hostname;
        const wsPort = '9000';
        const wsEndpoint = '/ws';
        const brokerURL = `${wsProtocol}://${wsHost}:${wsPort}${wsEndpoint}`;

        if (stompClientRef.current && stompClientRef.current.active) {
            console.log("STOMP (Basic): 새 연결 전 기존 클라이언트 비활성화.");
            stompClientRef.current.deactivate();
            stompClientRef.current = null;
        }

        const client = new Client({
            brokerURL,
            connectHeaders: {
                username: currentUser.name,
            },
            debug: (str) => {
                console.log(new Date(), 'STOMP DEBUG (Basic): ', str);
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            onConnect: (frame) => {
                setIsConnected(true);
                stompClientRef.current = client;
                stompClientRef.current.userIdBeforeDeactivation = currentUser.name;
                console.log(`STOMP (Basic) 연결 성공 (사용자: ${currentUser.name})`, frame);

                client.subscribe('/topic/connectedUsers', (message) => {
                    try {
                        const updatedConnectedUsersArray = JSON.parse(message.body);
                        setConnectedUsers(updatedConnectedUsersArray);
                        console.log("웹소켓을 통해 연결된 사용자 목록 업데이트됨: ", updatedConnectedUsersArray);

                        const debugUsersAfterUpdate = getDisplayedUsersInRightSidebarWithProvidedArray(updatedConnectedUsersArray, allUsers, currentUser);
                        console.log('*** 직후: 처리된 사이드바 사용자 (updatedConnectedUsersArray 사용):', debugUsersAfterUpdate);

                    } catch (error) {
                        console.error("연결된 사용자 업데이트 메시지 파싱 실패: ", message.body, error);
                    }
                });
                console.log("/topic/connectedUsers 토픽 구독됨.");
            },
            onStompError: (frame) => {
                console.error('STOMP (Basic): 브로커 오류 발생: ' + frame.headers['message'], frame.body);
                setIsConnected(false);
                // setConnectedUsers([]); // 이 부분은 초기 API 호출에서 처리하도록 유지
            },
            onWebSocketClose: (event) => {
                console.log('STOMP (Basic): 웹소켓 닫힘.', event);
                setIsConnected(false);
                // setConnectedUsers([]); // 이 부분은 초기 API 호출에서 처리하도록 유지
            },
            onDisconnect: (frame) => {
                console.log('STOMP (Basic): 클라이언트 연결 해제됨.', frame);
                setIsConnected(false);
                // setConnectedUsers([]); // 이 부분은 초기 API 호출에서 처리하도록 유지
            }
        });

        client.activate();

        return () => {
            if (stompClientRef.current && stompClientRef.current.active) {
                console.log(`STOMP 클라이언트 (Basic) 정리 중 비활성화 (사용자: ${currentUser.name}).`);
                stompClientRef.current.deactivate();
                stompClientRef.current = null;
                setIsConnected(false);
                // setConnectedUsers([]); // 이 부분은 초기 API 호출에서 처리하도록 유지
            }
        };
    }, [currentUser, allUsers]); // allUsers도 dependency에 추가하여 connectedUsers 업데이트 시 올바른 정보가 반영되도록 함

    // 채팅방 메시지 구독 (currentChatRoom이 있을 때만)
    useEffect(() => {
        if (!currentUser || !currentUser.id || !currentChatRoom || !stompClientRef.current || !stompClientRef.current.active) {
            console.log("STOMP (Messages): 메시지 구독을 위한 필수 조건 미충족. 현재 상태:", {
                currentUser: !!currentUser,
                currentChatRoom: !!currentChatRoom,
                stompClientActive: stompClientRef.current?.active
            });
            return;
        }

        const selectedRoomObject = chatRooms.find(room => room.id === currentChatRoom);
        if (!selectedRoomObject) {
            console.warn(`STOMP (Messages): currentChatRoom (${currentChatRoom})에 대한 방 데이터가 로컬 상태에서 발견되지 않음. 메시지 구독 연기.`);
            return;
        }

        const intRoomIdForSubscription = selectedRoomObject.intId;
        const subscriptionDestination = intRoomIdForSubscription <= PRIVATE_ROOM_MAX_ID_FROM_SERVER
            ? `/topic/private/${intRoomIdForSubscription}`
            : `/topic/group/${intRoomIdForSubscription}`;

        console.log(`STOMP (Messages): 방 ${currentChatRoom}의 메시지 구독 시도 중 (${subscriptionDestination})`);

        const subscription = stompClientRef.current.subscribe(subscriptionDestination, (message) => {
            try {
                const receivedMsg = JSON.parse(message.body);
                console.log(`방 ${currentChatRoom} (${subscriptionDestination} 구독)에서 메시지 수신:`, receivedMsg);
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
                console.error("STOMP 메시지 파싱 실패:", message.body, error);
            }
        });

        return () => {
            if (stompClientRef.current && stompClientRef.current.active && subscription) {
                console.log(`${subscriptionDestination} 구독 해제 중.`);
                subscription.unsubscribe();
            }
        };
    }, [currentChatRoom, currentUser, chatRooms, isConnected]);


    const handleSendMessage = () => {
        const selectedRoom = chatRooms.find(room => room.id === currentChatRoom);
        if (!selectedRoom) {
            console.error("메시지를 보낼 수 없습니다. 선택된 방 정보를 찾을 수 없습니다:", currentChatRoom);
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
                console.warn('사용자 정보가 로드되지 않았습니다.');
            } else if (!stompClientRef.current || !stompClientRef.current.active) {
                console.warn('채팅 서버에 연결되어 있지 않습니다.');
            } else {
                console.warn('빈 메시지는 보낼 수 없습니다.');
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


    // === 수정 부분: useCallback 없이 직접 함수 정의 ===
    const getDisplayedUsersInRightSidebar = () => {
        const currentConnectedUsersArray = Array.isArray(connectedUsers) ? connectedUsers : [];
        return getDisplayedUsersInRightSidebarWithProvidedArray(currentConnectedUsersArray, allUsers, currentUser);
    };


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
                                // currentChatRoom이 null이면 플레이스홀더 변경
                                placeholder={currentChatRoom ? `${getCurrentRoom()?.name}에 메시지 보내기...` : "메시지를 보낼 채팅방을 선택하거나 생성하세요..."}
                                className="message-input"
                                rows="1"
                                // currentChatRoom이 null이면 비활성화
                                disabled={!currentChatRoom}
                            />

                        <button className="input-action-button" disabled={!currentChatRoom}>
                            <Smile className="input-action-icon"/>
                        </button>

                        <button
                            onClick={handleSendMessage}
                            disabled={!newMessage.trim() || !currentChatRoom}
                            className={`send-button ${!newMessage.trim() || !currentChatRoom ? 'disabled' : 'enabled'}`}
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
                            title="사용자 목록 닫기"
                        >
                            <ChevronRight className="close-sidebar-icon"/>
                        </button>
                    </div>

                    <div className="participants-list">
                        <div className="participants-grid">
                            {getDisplayedUsersInRightSidebar().map((user) => (
                                <div key={user.id} className="participant-item">
                                    <div className="participant-avatar-container">
                                        <img src={user.avatar} alt={user.username} className="participant-avatar"/>
                                        <div className={`participant-status ${user.status === 'online' ? 'online' : 'offline'}`}></div>
                                    </div>
                                    <div className="participant-info">
                                        <p className="participant-name">{user.username}</p>
                                        <p className="participant-status-text">
                                            {user.status === 'online' ? '온라인' : '오프라인'}
                                        </p>
                                    </div>
                                </div>
                            ))}
                            {allUsersLoading && <div className="loading-text">사용자 목록 로딩 중...</div>}
                            {allUsersError && <div className="error-text">오류: {allUsersError}</div>}
                        </div>
                    </div>
                </div>
            )}

            {!isRightSidebarOpen && (
                <div className="sidebar-toggle">
                    <button
                        onClick={() => setIsRightSidebarOpen(true)}
                        className="toggle-button"
                        title="사용자 목록 열기"
                    >
                        <ChevronLeft className="toggle-icon"/>
                    </button>
                    <div className="toggle-text">사용자</div>
                </div>
            )}
        </div>
    );
};

export default Chat;

