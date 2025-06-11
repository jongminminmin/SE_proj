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
    const messageSubscriptionRef = useRef(null); // 메시지 구독을 위한 useRef 추가
    const readStatusSubscriptionRef = useRef(null); // 읽음 상태 구독을 위한 useRef 추가

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

    const [connectedUsers, setConnectedUsers] = useState([]);

    const [showChatTypeModal, setShowChatTypeModal] = useState(false);
    const [selectedUser, setSelectedUser] = useState(null);
    const [chatType, setChatType] = useState('private');
    const [groupName, setGroupName] = useState('');
    //우클릭 관련 상태 추가
    const [contextMenu, setContextMenu] = useState(null);
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [roomToDelete, setRoomToDelete] = useState(null);

    //상태 변수 추가
    const [messagesLoading, setMessagesLoading] = useState(false);
    const [lastReadMessageIds, setLastReadMessageIds] = useState({});// 각 방의 마지막 읽은 메시지 ID



    //우클릭 핸들러
    const handleRoomRightClick = (e, room) => {
        e.preventDefault();
        setContextMenu({
            x: e.clientX,
            y: e.clientY,
            room: room
        });
    };

    // 컨텍스트 메뉴 외부 클릭 시 닫기
    const handleCloseContextMenu = () => {
        setContextMenu(null);
    };

    // 삭제 확인 모달 열기
    const handleDeleteRoom = (room) => {
        setRoomToDelete(room);
        setShowDeleteConfirm(true);
        setContextMenu(null);
    };


    //채팅 유형 선택 모달
    const ChatTypeModal = () => (
        <div className="modal-overlay">
            <div className="modal-card">
                <h3>{selectedUser?.username}와의 채팅 생성</h3>
                <div className="form-group">
                    <label>
                        <input
                            type="radio"
                            value="private"
                            checked={chatType === 'private'}
                            onChange={e => setChatType(e.target.value)}
                        />
                        1:1 개인채팅
                    </label>
                    <label>
                        <input
                            type="radio"
                            value="group"
                            checked={chatType === 'group'}
                            onChange={e => setChatType(e.target.value)}
                        />
                        그룹채팅
                    </label>
                </div>

                {chatType === 'group' && (
                    <div className="form-group">
                        <input
                            text="text"
                            placeholder={groupName}
                            onChange={e => setGroupName(e.target.value)}
                            required
                        />
                    </div>
                )}
                <div className="modal-actions">
                    <button onClick={() => {
                        setShowChatTypeModal(false);
                        setGroupName('')
                    }}>취소</button>
                    <button onClick={handleCreateChatConfirm}>생성</button>
                </div>
            </div>
        </div>
    );

    const handleCreateChatConfirm = async () => {
        if(!currentUser || !selectedUser) return;

        const requestBody = {
            type: chatType.toUpperCase(),
            user2Id: selectedUser.id,
        };

        if (chatType === 'group') {
            requestBody.groupName = groupName;
            requestBody.participants = [currentUser.id, selectedUser.id];
        }

        try {
            const response = await fetch('/api/chats/create-room', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(requestBody)
            });

            if (response.ok) {
                const roomData = await response.json();
                setChatRooms(prev => [...prev, roomData]);
                setCurrentChatRoom(roomData.id);
                setShowChatTypeModal(false);
            }
        } catch (error) {
            console.error("채팅방 생성 실패:", error);
        }
    }

    //채팅방 삭제
    const handleConfirmDelete = async () => {
        if (!roomToDelete) return;

        try {
            const response = await fetch(`/api/chats/rooms/${roomToDelete.id}`, {
                method: 'DELETE',
                headers: {'Content-Type': 'application/json'}
            });

            if (response.ok) {
                // 삭제된 방을 채팅방 목록에서 제거
                setChatRooms(prev => prev.filter(room => room.id !== roomToDelete.id));

                // 현재 채팅방이 삭제된 방이면 다른 방으로 이동
                if (currentChatRoom === roomToDelete.id) {
                    const remainingRooms = chatRooms.filter(room => room.id !== roomToDelete.id);
                    setCurrentChatRoom(remainingRooms.length > 0 ? remainingRooms[0].id : null);
                }

                console.log(`채팅방 "${roomToDelete.name}" 삭제 완료`);
            } else {
                console.error("채팅방 삭제 실패:", response.status);
                // alert("채팅방 삭제에 실패했습니다."); // alert 대신 커스텀 모달 사용 권장
            }
        } catch (error) {
            console.error("채팅방 삭제 중 오류:", error);
            // alert("네트워크 오류로 채팅방을 삭제할 수 없습니다."); // alert 대신 커스텀 모달 사용 권장
        } finally {
            setShowDeleteConfirm(false);
            setRoomToDelete(null);
        }
    };

    //컨텍스트 메뉴 컴포넌트
    const ContextMenu = () => {
        if (!contextMenu) return null;

        return (
            <div
                className="context-menu"
                style={{
                    position: 'fixed',
                    top: contextMenu.y,
                    left: contextMenu.x,
                    zIndex: 1000
                }}
                onClick={(e) => e.stopPropagation()}
            >
                <div className="context-menu-item" onClick={() => handleDeleteRoom(contextMenu.room)}>
                    <span className="context-menu-icon">🗑️</span>
                    채팅방 삭제
                </div>
            </div>
        );
    };
    //삭제 확인 모달
    const DeleteConfirmModal = () => {
        if (!showDeleteConfirm || !roomToDelete) return null;

        return (
            <div className="modal-overlay" onClick={() => setShowDeleteConfirm(false)}>
                <div
                    className="modal-card"
                    onClick={(e) => e.stopPropagation()} // 모달 내부 클릭 시 전파 중단
                >
                    <h3>채팅방 삭제</h3>
                    <p>"{roomToDelete.name}" 채팅방을 삭제하시겠습니까?</p>
                    <p className="delete-warning">이 작업은 되돌릴 수 없습니다.</p>

                    <div className="modal-actions">
                        <button
                            className="cancel-button"
                            onClick={(e) => {
                                e.stopPropagation();
                                setShowDeleteConfirm(false);
                            }}
                        >
                            취소
                        </button>
                        <button
                            className="delete-button"
                            onClick={(e) => {
                                e.stopPropagation();
                                handleConfirmDelete();
                            }}
                        >
                            삭제
                        </button>
                    </div>
                </div>
            </div>
        );
    };


    // 채팅방 아이템 렌더링 부분 수정
    const renderRoomItem = (room) => (
        <div
            key={room.id}
            className={`room-item ${currentChatRoom === room.id ? 'active' : ''}`}
            onClick={() => handleRoomChange(room.id)}
            onContextMenu={(e) => handleRoomRightClick(e, room)} // 우클릭 이벤트 추가
        >
            <div className="room-content">
                <div className="room-avatar" style={{backgroundColor: room.color || '#6c757d'}}>
                    <Hash className="room-icon" />
                </div>
                <div className="room-info">
                    <div className="room-header">
                        <h4 className="room-name">{room.name}</h4>
                        {room.unreadCount > 0 && (
                            <span className="unread-badge">{room.unreadCount}</span>
                        )}
                    </div>
                    <p className="room-description">{room.description}</p>
                    <p className="room-last-message">{room.lastMessage}</p>
                    <div className="room-footer">
                        <span className="last-message-time">{room.lastMessageTime}</span>
                        <span className="participants-count">
            <Users className="participants-icon" />
                            {room.participants}명
          </span>
                    </div>
                </div>
            </div>
        </div>
    );

    //전역 클릭 이벤트 리스너
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (contextMenu) {
                //컨텍스트 메뉴 영역 클릭은 제외
                const contextMenuElement = event.target.closest('.context-menu');
                if(!contextMenuElement) {
                    setContextMenu(null);
                }
            }
        };

        document.addEventListener('click', handleClickOutside);
        return () => document.removeEventListener('click', handleClickOutside);
    }, [contextMenu]);




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

    // --- 채팅방 목록 불러오기 (unreadCount 반영) ---
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
    }, [currentUser, currentChatRoom]); // currentChatRoom을 의존성 배열에서 제거하여 불필요한 재호출 방지, handleRoomChange에서 수동 업데이트

    // 전체 사용자 목록 조회
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

    useEffect(() => {
        if (currentUser && currentUser.id) {
            const fetchInitialConnectedUsers = async () => {
                try {
                    const response = await fetch('/api/users/connected');
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
    }, [currentUser]);

    useEffect(() => {
        if (currentChatRoom && currentUser && currentUser.id) {
            const fetchRoomParticipants = async () => {
                setParticipantsLoading(true);
                try {
                    const response = await fetch(`/api/chats/rooms/${currentChatRoom}/participants`);
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

// --- STOMP 클라이언트 연결 로직 ---
    useEffect(() => {
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

        // 이미 연결되어 있고 사용자 변경이 없는 경우
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

        // 새 연결 전 기존 클라이언트 비활성화 (클린업 로직이 이미 처리하므로 불필요할 수 있지만 안전을 위해 유지)
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
            heartbeatIncoming: 15000, // 15초로 증가 (연결 안정성 향상)
            heartbeatOutgoing: 15000, // 15초로 증가 (연결 안정성 향상)
            onConnect: (frame) => {
                setIsConnected(true);
                stompClientRef.current = client; // 클라이언트 활성화 시 stompClientRef에 할당
                stompClientRef.current.userIdBeforeDeactivation = currentUser.name;
                console.log(`STOMP (Basic) 연결 성공 (사용자: ${currentUser.name})`, frame);

                // 연결된 사용자 목록 구독 (여기서만 구독하고, 다른 구독은 별도 useEffect로 분리)
                stompClientRef.current.subscribe('/topic/connectedUsers', (message) => {
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
                // 5초 후 자동 재연결 시도 (재연결 로직은 라이브러리 내장 또는 useEffect 재실행으로 처리)
            },
            onWebSocketClose: (event) => {
                console.log('STOMP (Basic): 웹소켓 닫힘.', event);
                setIsConnected(false);
            },
            onDisconnect: (frame) => {
                console.log('STOMP (Basic): 클라이언트 연결 해제됨.', frame);
                setIsConnected(false);
            }
        });

        client.activate();

        return () => {
            if (stompClientRef.current && stompClientRef.current.active) {
                console.log(`STOMP 클라이언트 (Basic) 정리 중 비활성화 (사용자: ${currentUser.name}).`);
                stompClientRef.current.deactivate();
                stompClientRef.current = null;
                setIsConnected(false);
            }
        };
    }, [currentUser, allUsers]);


    // --- 채팅방 메시지 구독 및 메시지 기록 불러오기 (분리된 useEffect) ---
    useEffect(() => {
        // 필수 조건: 현재 사용자 정보, 선택된 채팅방, 그리고 STOMP 클라이언트가 활성화되어 있어야 함
        if (!currentUser?.id || !currentChatRoom || !stompClientRef.current?.active) {
            console.log("STOMP (Messages): 메시지 구독을 위한 필수 조건 미충족. 현재 상태:", {
                currentUser: !!currentUser,
                currentChatRoom: !!currentChatRoom,
                stompClientActive: stompClientRef.current?.active
            });
            // 조건이 충족되지 않으면 기존 구독 해제
            if (messageSubscriptionRef.current) {
                messageSubscriptionRef.current.unsubscribe();
                messageSubscriptionRef.current = null;
            }
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

        // 기존 메시지 구독이 있으면 해제 (중복 방지)
        if (messageSubscriptionRef.current) {
            console.log("기존 메시지 구독 해제 중...");
            messageSubscriptionRef.current.unsubscribe();
            messageSubscriptionRef.current = null;
        }

        // 새 메시지 구독 설정
        messageSubscriptionRef.current = stompClientRef.current.subscribe(subscriptionDestination, (message) => {
            try {
                const receivedMsg = JSON.parse(message.body);
                console.log(`방 ${currentChatRoom} (${subscriptionDestination} 구독)에서 메시지 수신:`, receivedMsg);
                setMessages(prevMessages => {
                    const currentRoomMessages = prevMessages[currentChatRoom] || [];
                    // 중복 메시지 방지 로직 (messageId가 고유하다고 가정)
                    if (currentRoomMessages.find(msg => msg.id === receivedMsg.messageId)) {
                        return prevMessages;
                    }

                    const messageToStore = {
                        id: receivedMsg.messageId || Date.now(),
                        sender: receivedMsg.username || 'Unknown',
                        senderId: receivedMsg.senderId,
                        content: receivedMsg.content,
                        time: new Date(receivedMsg.timestamp).toLocaleTimeString('ko-KR', {
                            hour: 'numeric',
                            minute: '2-digit',
                            hour12: true
                        }),
                        isOwn: receivedMsg.senderId === currentUser.id,
                        avatar: receivedMsg.avatar || (receivedMsg.senderId === currentUser.id ? currentUser.avatar : 'https://via.placeholder.com/40'),
                        isRead: false, // 새로 받은 메시지는 읽지 않은 상태로 초기화 (자신이 보낸 메시지는 별도 처리)
                    };

                    // 자신이 보낸 메시지는 즉시 '읽음'으로 처리
                    if (messageToStore.isOwn) {
                        messageToStore.isRead = true;
                    }

                    return {
                        ...prevMessages,
                        [currentChatRoom]: [...currentRoomMessages, messageToStore]
                    };
                });
                scrollToBottom();
            } catch (error) {
                console.error("STOMP 메시지 파싱 실패:", message.body, error);
            }
        });

        console.log(`STOMP (Messages): ${subscriptionDestination} 구독 완료.`);


        // Effect 클린업 함수
        return () => {
            if (messageSubscriptionRef.current) {
                console.log(`STOMP (Messages): ${subscriptionDestination} 구독 해제 중.`);
                messageSubscriptionRef.current.unsubscribe();
                messageSubscriptionRef.current = null;
            }
        };
    }, [currentChatRoom, currentUser, isConnected, chatRooms, scrollToBottom]); // isConnected 추가


    // 읽음 상태 구독 추가
    useEffect(() => {
        // 필수 조건: 현재 사용자 정보, 선택된 채팅방, 그리고 STOMP 클라이언트가 활성화되어 있어야 함
        if (!currentUser?.id || !currentChatRoom || !stompClientRef.current?.active) {
            console.log("STOMP (Read Status): 읽음 상태 구독을 위한 필수 조건 미충족.");
            // 조건이 충족되지 않으면 기존 구독 해제
            if (readStatusSubscriptionRef.current) {
                readStatusSubscriptionRef.current.unsubscribe();
                readStatusSubscriptionRef.current = null;
            }
            return;
        }

        // 기존 읽음 상태 구독이 있으면 해제 (중복 방지)
        if (readStatusSubscriptionRef.current) {
            console.log("기존 읽음 상태 구독 해제 중...");
            readStatusSubscriptionRef.current.unsubscribe();
            readStatusSubscriptionRef.current = null;
        }

        console.log(`STOMP (Read Status): 방 ${currentChatRoom}의 읽음 상태 구독 시도 중.`);

        readStatusSubscriptionRef.current = stompClientRef.current.subscribe(
            `/topic/readStatus/${currentChatRoom}`,
            (message) => {
                try {
                    const readData = JSON.parse(message.body);
                    console.log("읽음 상태 메시지 수신:", readData);

                    // 다른 사용자가 읽음 처리했을 때 내 메시지들을 읽음으로 표시
                    if (readData.userId !== currentUser.id) {
                        setMessages(prev => {
                            const currentMessages = prev[currentChatRoom] || [];
                            const updatedMessages = currentMessages.map(msg => {
                                // 자신이 보낸 메시지 중 아직 읽지 않은 메시지를 '읽음'으로 업데이트
                                if (msg.senderId === currentUser.id && !msg.isRead) {
                                    return { ...msg, isRead: true };
                                }
                                return msg;
                            });
                            return { ...prev, [currentChatRoom]: updatedMessages };
                        });
                        console.log(`${readData.username}이 메시지를 읽음 처리했습니다.`);
                    }
                } catch (error) {
                    console.error("읽음 상태 업데이트 실패:", error);
                }
            }
        );

        console.log(`STOMP (Read Status): /topic/readStatus/${currentChatRoom} 구독 완료.`);

        return () => {
            if (readStatusSubscriptionRef.current) {
                console.log(`STOMP (Read Status): /topic/readStatus/${currentChatRoom} 구독 해제 중.`);
                readStatusSubscriptionRef.current.unsubscribe();
                readStatusSubscriptionRef.current = null;
            }
        };
    }, [currentChatRoom, currentUser, isConnected]); // isConnected 추가


    // 메시지 입력 및 전송 로직은 변경 없음
    const handleSendMessage = () => {
        const selectedRoom = chatRooms.find(room => room.id === currentChatRoom);
        if (!selectedRoom) {
            console.error("메시지를 보낼 수 없습니다. 선택된 방 정보를 찾을 수 없습니다:", currentChatRoom);
            return;
        }

        if (newMessage.trim() && currentUser && stompClientRef.current && stompClientRef.current.active) {
            const messagePayload = {
                senderId: currentUser.id, // currentUser.id는 String
                chatRoomId: selectedRoom.id, // DTO의 chatRoomId는 String
                username: currentUser.name,
                content: newMessage.trim(),
                timestamp: new Date().toISOString(), // ISO String으로 보냄
            };

            stompClientRef.current.publish({
                destination: `/app/chat.sendMessage/${currentChatRoom}`, // PathVariable은 String ID 사용
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

    const handleRoomChange = useCallback(async (roomId) => {
        if (roomId === currentChatRoom) return;

        setMessagesLoading(true);
        setCurrentChatRoom(roomId);

        // 메시지 내역 초기화 후 로드 (UI에 즉시 반영)
        setMessages(prev => ({
            ...prev,
            [roomId]: [] // 새로운 방 선택 시 메시지 목록을 비워둡니다.
        }));

        try {
            // 1. 채팅 내역 불러오기
            const response = await fetch(`/api/chats/rooms/${roomId}/messages`);
            if (response.ok) {
                const historyData = await response.json();
                const formattedHistory = historyData.map(msg => ({
                    id: msg.messageId,
                    sender: msg.username,
                    senderId: msg.senderId,
                    content: msg.content,
                    time: new Date(msg.timestamp).toLocaleTimeString('ko-KR', {
                        hour: 'numeric',
                        minute: '2-digit',
                        hour12: true
                    }),
                    isOwn: msg.senderId === currentUser.id,
                    isRead: true, // 로드된 기존 메시지는 모두 읽음 처리
                    avatar: 'https://via.placeholder.com/40'
                }));

                setMessages(prev => ({
                    ...prev,
                    [roomId]: formattedHistory
                }));

                // 마지막 메시지 ID 저장
                if (formattedHistory.length > 0) {
                    const lastMessage = formattedHistory[formattedHistory.length - 1];
                    setLastReadMessageIds(prev => ({
                        ...prev,
                        [roomId]: lastMessage.id
                    }));
                }
                scrollToBottom(); // 메시지 로드 후 스크롤
            } else {
                console.error(`채팅방 ${roomId} 메시지 기록 로드 실패:`, response.status, await response.text());
                setMessages(prev => ({ ...prev, [roomId]: [] })); // 실패 시 메시지 목록 비우기
            }

            // 2. 읽음 처리 API 호출 후 웹소켓으로 알림
            const readResponse = await fetch(`/api/chats/rooms/${roomId}/mark-as-read`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'}
            });

            if (readResponse.ok) {
                // 채팅방 목록의 unreadCount 업데이트
                setChatRooms(prev => prev.map(room =>
                    room.id === roomId ? {...room, unreadCount: 0} : room
                ));

                // 다른 사용자들에게 읽음 상태 전송
                if (stompClientRef.current?.active) {
                    stompClientRef.current.publish({
                        destination: `/app/chat.markAsRead/${roomId}`,
                        body: JSON.stringify({
                            userId: currentUser.id,
                            roomId: roomId,
                            timestamp: new Date().toISOString(),
                            username: currentUser.name
                        })
                    });
                }
            } else {
                console.error(`채팅방 ${roomId} 읽음 처리 실패:`, readResponse.status, await readResponse.text());
            }
        } catch (error) {
            console.error("채팅방 전환 중 오류:", error);
            setMessages(prev => ({ ...prev, [roomId]: [] })); // 오류 발생 시 메시지 목록 비우기
        } finally {
            setMessagesLoading(false);
        }

    },[currentUser, scrollToBottom]); // currentChatRoom 제거

    const getCurrentRoom = () => chatRooms.find(room => room.id === currentChatRoom);

    const getCurrentParticipants = () => {
        const currentRoom = chatRooms.find(room => room.id === currentChatRoom);
        return currentRoom ? currentRoom.participants : 0; // ChatRoomDTO의 participants는 Long
    };

    const getDisplayedUsersInRightSidebar = () => {
        const currentConnectedUsersArray = Array.isArray(connectedUsers) ? connectedUsers : [];
        return getDisplayedUsersInRightSidebarWithProvidedArray(currentConnectedUsersArray, allUsers, currentUser);
    };

    // --- 사이드바 사용자 클릭 핸들러 ---
    const handleUserClickForChat = (user) => {
        if(user.id === currentUser.id) {
            console.log("자신과의 채팅은 허용되지 않습니다.");
            return;
        }

        if(user.status !== 'online'){
            // alert(`${user.username}님은 현재 오프라인으로 채팅을 시작할 수 없습니다.`); // alert 대신 커스텀 모달 사용 권장
            console.warn(`${user.username}님은 현재 오프라인으로 채팅을 시작할 수 없습니다.`);
            return;
        }

        setSelectedUser(user);
        setShowChatTypeModal(true);
    };

    //메시지 렌더링 부분
    const renderMessage = useCallback((message) => (
        <div key={message.id} className={`message ${message.isOwn ? 'own' : 'other'}`}>
            <div className="message-content">
                {!message.isOwn && (
                    <img src={message.avatar} alt="avatar" className="message-avatar" />
                )}
                <div className="message-body">
                    {!message.isOwn && (
                        <div className="message-sender">{message.sender}</div>
                    )}
                    <div className={`message-bubble ${message.isOwn ? 'own-bubble' : 'other-bubble'}`}>
                        <p className="message-text">{message.content}</p>
                    </div>
                    <div className="message-footer">
                        <div className="message-time">{message.time}</div>
                        {message.isOwn && (
                            <div className={`read-status ${message.isRead ? 'read' : 'unread'}`}>
                                {message.isRead ? '읽음' : '1'}
                            </div>
                        )}
                    </div>
                </div>
                {message.isOwn && (
                    <img src={currentUser?.avatar} alt="avatar" className="message-avatar" />
                )}
            </div>
        </div>
    ), [currentUser]);

// 메시지 목록 렌더링
    const getCurrentMessages = useCallback(() =>
        messages[currentChatRoom] || [], [messages, currentChatRoom]);

    return (
        <>
            {showChatTypeModal && <ChatTypeModal />}
            <ContextMenu/>
            <DeleteConfirmModal/>

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
                        {!chatRoomsLoading && !chatRoomsError && chatRooms.length > 0 && chatRooms.map(renderRoomItem)}
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
                                <p className="current-room-participants">{getCurrentParticipants()?? 0}명 참여 중</p>
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
                        {messagesLoading ? (
                            <div className="loading-text">메시지 로딩 중...</div>
                        ) : getCurrentMessages().length === 0 ? (
                            // 빈 채팅방 상태
                            <div className="empty-chat">
                                <div className="empty-chat-avatar" style={{backgroundColor: getCurrentRoom()?.color}}>
                                    <Hash className="empty-chat-icon"/>
                                </div>
                                <h3 className="empty-chat-title">{getCurrentRoom()?.name || '채팅방 없음'}</h3>
                                <p className="empty-chat-description">
                                    {getCurrentRoom()?.description}<br/>
                                    팀원들과 소통을 시작해보세요.
                                </p>
                            </div>
                        ) : (
                            // 메시지 목록
                            <div className="messages-list">
                                {(getCurrentMessages() || []).map(renderMessage)}
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
                                <h3 className="participants-title">모든 사용자</h3>
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
                                    <div
                                        key={user.id}
                                        className="participant-item"
                                        onClick={() => handleUserClickForChat(user)}
                                    >
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
        </>
    );
};

export default Chat;
