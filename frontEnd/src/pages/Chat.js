import React, {useState, useRef, useEffect, useCallback, use} from 'react';
import { Client } from '@stomp/stompjs';
import { useNavigate } from "react-router-dom";

// SVG 아이콘 컴포넌트들
const Send = ({ style }) => (
    <svg style={{ width: '20px', height: '20px', ...style }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
    </svg>
);

const Smile = ({ style }) => (
    <svg style={{ width: '20px', height: '20px', ...style }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="10"/>
        <path d="m9 9 6 0"/>
        <path d="m9 15 6 0"/>
    </svg>
);

const MoreVertical = ({ style }) => (
    <svg style={{ width: '20px', height: '20px', ...style }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="1"/>
        <circle cx="12" cy="5" r="1"/>
        <circle cx="12" cy="19" r="1"/>
    </svg>
);

const Hash = ({ style }) => (
    <svg style={{ width: '24px', height: '24px', ...style }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <line x1="4" y1="9" x2="20" y2="9"/>
        <line x1="4" y1="15" x2="20" y2="15"/>
        <line x1="10" y1="3" x2="8" y2="21"/>
        <line x1="16" y1="3" x2="14" y2="21"/>
    </svg>
);

const Users = ({ style }) => (
    <svg style={{ width: '20px', height: '20px', ...style }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
        <circle cx="9" cy="7" r="4"/>
        <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
        <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
    </svg>
);

const ChevronLeft = ({ style }) => (
    <svg style={{ width: '16px', height: '16px', ...style }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
    </svg>
);

const ChevronRight = ({ style }) => (
    <svg style={{ width: '16px', height: '16px', ...style }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
    </svg>
);

const PRIVATE_ROOM_MAX_ID_FROM_SERVER = 10;

const Chat = () => {
    const [messages, setMessages] = useState({});
    const [newMessage, setNewMessage] = useState('');
    const [isConnected, setIsConnected] = useState(false);
    const stompClientRef = useRef(null);

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

    //웹소켓 연결 확인
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
                        name: userData.username,
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
                            setCurrentChatRoom(null);
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
                    const response = await fetch(`/api/users/api/chat/`);
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

    // 특정 채팅방 참여자 목록 조회
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

    // STOMP 웹소켓 연결
    useEffect(() => {
        if (!currentUser || !currentUser.id || !currentChatRoom) {
            if (stompClientRef.current && stompClientRef.current.active) {
                stompClientRef.current.deactivate();
            }
            setIsConnected(false);
            return;
        }

        const selectedRoomObject = chatRooms.find(room => room.id === currentChatRoom);
        if (!selectedRoomObject) {
            console.warn(`STOMP: Room data not found in local state for currentChatRoom: ${currentChatRoom}. Connection deferred.`);
            return;
        }

        const intRoomIdForSubscription = selectedRoomObject.intId;
        if (stompClientRef.current && stompClientRef.current.active) {
            // 방이 변경되었거나 사용자가 변경된 경우 기존 연결 해제 후 새 연결
            if (stompClientRef.current.roomIdBeforeDeactivation !== currentChatRoom ||
                stompClientRef.current.userIdBeforeDeactivation !== currentUser.id) {
                console.log(`Deactivating existing STOMP client for room: ${stompClientRef.current.roomIdBeforeDeactivation} user: ${stompClientRef.current.userIdBeforeDeactivation}`);
                stompClientRef.current.deactivate();
            } else {
                // 이미 올바른 방/사용자로 연결되어 있다면 추가 작업 불필요
                console.log("STOMP: Already connected to the correct room and user.");
                if (!isConnected) setIsConnected(true);
                return;
            }
        }

        console.log(`STOMP: Attempting to connect for room ${currentChatRoom} (intId: ${intRoomIdForSubscription}) user ${currentUser.id}`);

        const wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
        const wsHost = window.location.hostname;
        const wsPort = process.env.REACT_APP_WS_PORT || '9443';
        const wsEndpoint = process.env.REACT_APP_WS_ENDPOINT || '/ws';
        const brokerURL = `${wsProtocol}://${wsHost}:${wsPort}${wsEndpoint}`;

        const client = new Client({
            brokerURL,
            connectHeaders: {
                username: currentUser.name,
            },
            debug: (str) => {
                console.log(new Date(), 'STOMP DEBUG: ', str);
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            onConnect: (frame) => {
                setIsConnected(true);
                stompClientRef.current = client;
                stompClientRef.current.roomIdBeforeDeactivation = currentChatRoom;
                stompClientRef.current.userIdBeforeDeactivation = currentUser.id;
                console.log(`STOMP Connected (room string ID: ${currentChatRoom}, intID: ${intRoomIdForSubscription}) as user: ${currentUser.id}`, frame);


                client.subscribe('/topic/connectedUsers', (messages) =>{
                    try{
                        //백엔드에서 보낸 connectedUsers Set<String>을 JSON으로 파싱
                        const updatedConnectedUsersArray = JSON.parse(messages.body);
                        setConnectedUsers(new Set(updatedConnectedUsersArray));
                        console.log("Updated connected users via WebSocket: ", updatedConnectedUsersArray)
                    } catch (error){
                        console.error("Failed to parse connected users update message: ", messages.body, error);
                    }
                });

                console.log("Subscribed to /topic/connectedUsers for real-time updates.");



                let subscriptionDestination;
                if (intRoomIdForSubscription <= PRIVATE_ROOM_MAX_ID_FROM_SERVER) {
                    subscriptionDestination = `/topic/private/${intRoomIdForSubscription}`;
                } else {
                    subscriptionDestination = `/topic/group/${intRoomIdForSubscription}`;
                }

                client.subscribe(subscriptionDestination, (message) => {
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

                console.log(`Subscribed to ${subscriptionDestination}`);
            },
            onStompError: (frame) => {
                console.error('STOMP: Broker reported error: ' + frame.headers['message'], frame.body);
                setIsConnected(false);
            },
            onWebSocketClose: (event) => {
                console.log('STOMP: WebSocket closed.', event);
                setIsConnected(false);
            },
            onDisconnect: (frame) => {
                console.log('STOMP: Client disconnected.', frame);
                setIsConnected(false);
            }
        });

        client.activate();

        return () => {
            if (client && client.active) {
                console.log(`Deactivating STOMP client for room: ${currentChatRoom} (or ${stompClientRef.current?.roomIdBeforeDeactivation}) user: ${currentUser.id} during cleanup.`);
                client.deactivate();
                stompClientRef.current = null;
            }
        };
    }, [currentChatRoom, currentUser, chatRooms]);

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

    const getDisplayedUsersInRightSidebar = useCallback(() => {
        if (!allUsers || allUsers.length === 0) {
            return [];
        }

        const usersWithStatus = allUsers.map(u => ({
            ...u,
            // connectedUsers Set에 해당 사용자 ID (또는 여기서는 username)가 있는지 확인하여 'online' 상태 설정
            // 백엔드 WebSocketSessionTracker가 username을 저장하므로, connectedUsers에는 username이 들어있을 것입니다.
            // 따라서, u.username (또는 u.id.toString()이 아니라면 u.username)을 사용해야 합니다.
            status: connectedUsers.has(u.username) ? 'online' : 'offline', // changed from u.id.toString() to u.username
            avatar: u.avatar || 'https://via.placeholder.com/32/CCCCCC/FFFFFF/?text=' + u.username.charAt(0).toUpperCase()
        }));

        let sortedUsers = [];
        if (currentUser) {
            // currentUser의 id와 비교할 때는 여전히 id를 사용해야 합니다.
            // allUsers의 u.id는 DB의 ID, connectedUsers는 username.
            // 따라서, connectedUsers.has(currentUser.name)으로 현재 사용자 상태를 확인합니다.
            const otherUsers = usersWithStatus.filter(u => u.id !== currentUser.id.toString());
            const me = usersWithStatus.find(u => u.id === currentUser.id.toString());

            if (me) {
                sortedUsers.push({
                    ...me,
                    avatar: currentUser.avatar,
                    name: currentUser.name,
                    status: connectedUsers.has(currentUser.name) ? 'online' : 'offline' // changed to currentUser.name
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
            return a.name.localeCompare(b.name);
        });

        return sortedUsers;


    },[allUsers, connectedUsers, currentUser]);


    return (
        <div style={{
            display: 'flex',
            height: '100vh',
            backgroundColor: '#F9FAFB',
            fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif'
        }}>
            {/* 왼쪽 사이드바 - 채팅방 목록 */}
            <div style={{
                width: '320px',
                backgroundColor: 'white',
                borderRight: '1px solid #E5E7EB',
                display: 'flex',
                flexDirection: 'column'
            }}>
                <div style={{
                    padding: '16px',
                    borderBottom: '1px solid #E5E7EB'
                }}>
                    <h2 style={{fontSize: '18px', fontWeight: '600', color: '#1F2937', margin: 0}}>프로젝트 채팅</h2>
                    <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        marginTop: '8px'
                    }}>
                        <div style={{
                            width: '8px',
                            height: '8px',
                            borderRadius: '50%',
                            marginRight: '8px',
                            backgroundColor: isConnected ? '#10B981' : '#EF4444'
                        }}></div>
                        <span style={{fontSize: '12px', color: '#6B7280'}}>
              {isConnected ? '연결됨' : '연결 중...'}
            </span>
                    </div>
                </div>

                <div style={{
                    flex: 1,
                    overflowY: 'auto',
                    padding: '8px'
                }}>
                    {chatRooms.map((room) => (
                        <div
                            key={room.id}
                            onClick={() => handleRoomChange(room.id)}
                            style={{
                                padding: '12px',
                                borderRadius: '8px',
                                marginBottom: '8px',
                                cursor: 'pointer',
                                backgroundColor: currentChatRoom === room.id ? '#EFF6FF' : 'transparent',
                                borderLeft: currentChatRoom === room.id ? '4px solid #3B82F6' : 'none'
                            }}
                        >
                            <div style={{display: 'flex', alignItems: 'flex-start', gap: '12px'}}>
                                <div style={{
                                    width: '48px',
                                    height: '48px',
                                    backgroundColor: room.color,
                                    borderRadius: '8px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    flexShrink: 0
                                }}>
                                    <Hash style={{color: 'white'}}/>
                                </div>
                                <div style={{flex: 1, minWidth: 0}}>
                                    <div style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'space-between'
                                    }}>
                                        <h3 style={{
                                            fontSize: '14px',
                                            fontWeight: '500',
                                            color: '#1F2937',
                                            margin: 0,
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis',
                                            whiteSpace: 'nowrap'
                                        }}>{room.name}</h3>
                                        {room.unreadCount > 0 && (
                                            <span style={{
                                                backgroundColor: '#EF4444',
                                                color: 'white',
                                                fontSize: '11px',
                                                borderRadius: '12px',
                                                padding: '2px 8px',
                                                marginLeft: '8px'
                                            }}>
                        {room.unreadCount}
                      </span>
                                        )}
                                    </div>
                                    <p style={{
                                        fontSize: '12px',
                                        color: '#6B7280',
                                        margin: '4px 0',
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                        whiteSpace: 'nowrap'
                                    }}>{room.description}</p>
                                    <p style={{
                                        fontSize: '12px',
                                        color: '#9CA3AF',
                                        margin: '4px 0',
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                        whiteSpace: 'nowrap'
                                    }}>{room.lastMessage}</p>
                                    <div style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'space-between',
                                        marginTop: '8px'
                                    }}>
                                        <span style={{fontSize: '11px', color: '#9CA3AF'}}>{room.lastMessageTime}</span>
                                        <div style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            fontSize: '11px',
                                            color: '#9CA3AF'
                                        }}>
                                            <Users style={{width: '12px', height: '12px', marginRight: '4px'}}/>
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
            <div style={{
                flex: 1,
                display: 'flex',
                flexDirection: 'column'
            }}>
                {/* 채팅 헤더 */}
                <div style={{
                    backgroundColor: 'white',
                    borderBottom: '1px solid #E5E7EB',
                    padding: '16px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between'
                }}>
                    <div style={{display: 'flex', alignItems: 'center', gap: '12px'}}>
                        <div style={{
                            width: '40px',
                            height: '40px',
                            backgroundColor: getCurrentRoom()?.color,
                            borderRadius: '8px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center'
                        }}>
                            <Hash style={{color: 'white'}}/>
                        </div>
                        <div>
                            <h3 style={{
                                fontSize: '16px',
                                fontWeight: '600',
                                color: '#1F2937',
                                margin: 0
                            }}>{getCurrentRoom()?.name}</h3>
                            <p style={{fontSize: '14px', color: '#6B7280', margin: 0}}>{getCurrentParticipants().length}명
                                참여 중</p>
                        </div>
                    </div>

                    <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                        <button
                            onClick={() => setIsRightSidebarOpen(!isRightSidebarOpen)}
                            style={{
                                padding: '8px',
                                borderRadius: '50%',
                                border: 'none',
                                cursor: 'pointer',
                                backgroundColor: 'transparent'
                            }}
                            title={isRightSidebarOpen ? "참여자 목록 숨기기" : "참여자 목록 보기"}
                        >
                            <Users style={{color: '#6B7280'}}/>
                        </button>
                        <button style={{
                            padding: '8px',
                            borderRadius: '50%',
                            border: 'none',
                            cursor: 'pointer',
                            backgroundColor: 'transparent'
                        }}>
                            <MoreVertical style={{color: '#6B7280'}}/>
                        </button>
                    </div>
                </div>

                {/* 메시지 영역 */}
                <div style={{
                    flex: 1,
                    overflowY: 'auto',
                    padding: '16px'
                }}>
                    {getCurrentMessages().length === 0 ? (
                        // 빈 채팅방 상태
                        <div style={{
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            justifyContent: 'center',
                            height: '100%',
                            color: '#6B7280'
                        }}>
                            <div style={{
                                width: '64px',
                                height: '64px',
                                backgroundColor: getCurrentRoom()?.color,
                                borderRadius: '50%',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                marginBottom: '16px'
                            }}>
                                <Hash style={{color: 'white', width: '32px', height: '32px'}}/>
                            </div>
                            <h3 style={{
                                fontSize: '18px',
                                fontWeight: '500',
                                marginBottom: '8px'
                            }}>{getCurrentRoom()?.name}</h3>
                            <p style={{fontSize: '14px', textAlign: 'center', lineHeight: '1.5'}}>
                                {getCurrentRoom()?.description}<br/>
                                팀원들과 소통을 시작해보세요.
                            </p>
                        </div>
                    ) : (
                        // 메시지 목록
                        <div>
                            {getCurrentMessages().map((message) => (
                                <div key={message.id} style={{
                                    display: 'flex',
                                    justifyContent: message.isOwn ? 'flex-end' : 'flex-start',
                                    marginBottom: '16px'
                                }}>
                                    <div style={{
                                        display: 'flex',
                                        alignItems: 'flex-end',
                                        gap: '8px',
                                        flexDirection: message.isOwn ? 'row-reverse' : 'row',
                                        maxWidth: '70%'
                                    }}>
                                        <img
                                            src={message.avatar}
                                            alt={message.sender}
                                            style={{width: '32px', height: '32px', borderRadius: '50%', flexShrink: 0}}
                                        />
                                        <div style={{
                                            display: 'flex',
                                            flexDirection: 'column',
                                            alignItems: message.isOwn ? 'flex-end' : 'flex-start'
                                        }}>
                                            {!message.isOwn && (
                                                <span style={{
                                                    fontSize: '12px',
                                                    color: '#6B7280',
                                                    marginBottom: '4px',
                                                    paddingLeft: '4px'
                                                }}>{message.sender}</span>
                                            )}
                                            <div style={{
                                                padding: '12px 16px',
                                                borderRadius: '18px',
                                                backgroundColor: message.isOwn ? '#3B82F6' : '#E5E7EB',
                                                color: message.isOwn ? 'white' : '#1F2937'
                                            }}>
                                                <p style={{
                                                    fontSize: '14px',
                                                    lineHeight: '1.5',
                                                    margin: 0
                                                }}>{message.content}</p>
                                            </div>
                                            <span style={{
                                                fontSize: '11px',
                                                color: '#9CA3AF',
                                                marginTop: '4px',
                                                paddingLeft: '4px'
                                            }}>{message.time}</span>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                    <div ref={messagesEndRef}/>
                </div>

                {/* 메시지 입력 영역 */}
                <div style={{
                    backgroundColor: 'white',
                    borderTop: '1px solid #E5E7EB',
                    padding: '16px'
                }}>
                    <div style={{
                        display: 'flex',
                        alignItems: 'flex-end',
                        gap: '12px'
                    }}>
            <textarea
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder={`${getCurrentRoom()?.name}에 메시지 보내기...`}
                style={{
                    flex: 1,
                    backgroundColor: '#F3F4F6',
                    borderRadius: '20px',
                    padding: '12px 16px',
                    border: 'none',
                    outline: 'none',
                    resize: 'none',
                    fontSize: '14px',
                    minHeight: '24px',
                    maxHeight: '100px'
                }}
                rows="1"
            />

                        <button style={{
                            padding: '8px',
                            borderRadius: '50%',
                            border: 'none',
                            cursor: 'pointer',
                            backgroundColor: 'transparent'
                        }}>
                            <Smile style={{color: '#6B7280'}}/>
                        </button>

                        <button
                            onClick={handleSendMessage}
                            disabled={!newMessage.trim()}
                            style={{
                                padding: '8px',
                                borderRadius: '50%',
                                border: 'none',
                                cursor: !newMessage.trim() ? 'not-allowed' : 'pointer',
                                backgroundColor: !newMessage.trim() ? '#D1D5DB' : '#3B82F6',
                                color: !newMessage.trim() ? '#6B7280' : 'white'
                            }}
                        >
                            <Send style={{color: 'inherit'}}/>
                        </button>
                    </div>
                </div>
            </div>


            {/* 오른쪽 사이드 바 - 모든 사용자 목록 (온라인 상태 포함)*/}
            {isRightSidebarOpen && (
                <div style={{
                    width: '256px',
                    backgroundColor: 'white',
                    borderLeft: '1px solid #E5E7EB',
                    display: 'flex',
                    flexDirection: 'column'
                }}>
                    <div style={{
                        padding: '16px',
                        borderBottom: '1px solid #E5E7EB',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between'
                    }}>
                        <div>
                            {/* 텍스트 변경: "참여자" -> "모든 사용자" 또는 "사용자" */}
                            <h3 style={{fontSize: '16px', fontWeight: '600', color: '#1F2937', margin: 0}}>모든 사용자</h3>
                            {/* 현재 사이드바에 표시되는 사용자 수를 getDisplayedUsersInRightSidebar().length로 표시 */}
                            <p style={{
                                fontSize: '14px',
                                color: '#6B7280',
                                margin: 0
                            }}>{getDisplayedUsersInRightSidebar().length}명</p>
                        </div>
                        <button
                            onClick={() => setIsRightSidebarOpen(false)}
                            style={{
                                padding: '4px',
                                backgroundColor: 'transparent',
                                border: 'none',
                                cursor: 'pointer',
                                borderRadius: '4px'
                            }}
                            title="사용자 목록 닫기" // 툴팁 텍스트 변경
                        >
                            <ChevronRight style={{color: '#6B7280'}}/>
                        </button>
                    </div>

                    <div style={{
                        flex: 1,
                        overflowY: 'auto',
                        padding: '16px'
                    }}>
                        <div style={{display: 'flex', flexDirection: 'column', gap: '12px'}}>
                            {/* getCurrentParticipants() 대신 getDisplayedUsersInRightSidebar() 사용 */}
                            {getDisplayedUsersInRightSidebar().map((user) => (
                                <div key={user.id} style={{ // key는 user.id로 유지
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '12px',
                                    padding: '8px',
                                    borderRadius: '8px'
                                }}>
                                    <div style={{position: 'relative'}}>
                                        <img src={user.avatar} alt={user.name}
                                             style={{width: '32px', height: '32px', borderRadius: '50%'}}/>
                                        <div style={{
                                            position: 'absolute',
                                            bottom: '-2px',
                                            right: '-2px',
                                            width: '12px',
                                            height: '12px',
                                            borderRadius: '50%',
                                            border: '2px solid white',
                                            // user.status는 'online' 또는 'offline'이 될 것입니다.
                                            // 'away' 상태는 connectedUsers만으로는 알 수 없으므로, 제거하거나 별도 로직이 필요합니다.
                                            backgroundColor: user.status === 'online' ? '#10B981' : '#6B7280' // 'away' 제거
                                        }}></div>
                                    </div>
                                    <div style={{flex: 1}}>
                                        <p style={{
                                            fontSize: '14px',
                                            fontWeight: '500',
                                            color: '#1F2937',
                                            margin: 0
                                        }}>{user.name}</p>
                                        <p style={{fontSize: '12px', color: '#6B7280', margin: 0}}>
                                            {/* user.status는 'online' 또는 'offline'이므로 조건 변경 */}
                                            {user.status === 'online' ? '온라인' : '오프라인'}
                                        </p>
                                    </div>
                                </div>
                            ))}
                            {/* 로딩 및 에러 메시지 추가 (getDisplayedUsersInRightSidebar 함수에 사용된 allUsersLoading/allUsersError 상태 참조) */}
                            {allUsersLoading && <div style={{fontSize: '14px', color: '#6B7280', textAlign: 'center', padding: '16px'}}>사용자 목록 로딩 중...</div>}
                            {allUsersError && <div style={{fontSize: '14px', color: '#EF4444', textAlign: 'center', padding: '16px'}}>오류: {allUsersError}</div>}
                        </div>
                    </div>
                </div>
            )}

            {/* 사이드바가 닫혔을 때 토글 버튼 */}
            {!isRightSidebarOpen && (
                <div style={{
                    width: '48px',
                    backgroundColor: 'white',
                    borderLeft: '1px solid #E5E7EB',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    padding: '16px 0'
                }}>
                    <button
                        onClick={() => setIsRightSidebarOpen(true)}
                        style={{
                            padding: '8px',
                            backgroundColor: 'transparent',
                            border: 'none',
                            cursor: 'pointer',
                            borderRadius: '50%'
                        }}
                        title="참여자 목록 열기"
                    >
                        <ChevronLeft style={{color: '#6B7280'}}/>
                    </button>
                    <div style={{
                        marginTop: '8px',
                        fontSize: '12px',
                        color: '#6B7280',
                        writingMode: 'vertical-rl',
                        textOrientation: 'mixed'
                    }}>
                        참여자
                    </div>
                </div>
            )}
        </div>
    );
};

export default Chat;