import React, { useState, useRef, useEffect } from 'react';

// 기본 아이콘 컴포넌트들
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

const Chat = () => {
    const [messages, setMessages] = useState({});
    const [newMessage, setNewMessage] = useState('');
    const [isConnected, setIsConnected] = useState(false);
    const [socket, setSocket] = useState(null);
    const [currentChatRoom, setCurrentChatRoom] = useState('project_001');
    const [isRightSidebarOpen, setIsRightSidebarOpen] = useState(true);
    const [currentUser] = useState({
        id: 'user_001',
        name: '나',
        avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=40&h=40&fit=crop&crop=face'
    });

    // 프로젝트별 채팅방 목록
    const [chatRooms] = useState([
        {
            id: 'project_001',
            name: '웹 애플리케이션 개발',
            description: '메인 프로젝트 채팅방',
            lastMessage: '개발 일정 논의 필요합니다',
            lastMessageTime: '오후 3:42',
            unreadCount: 2,
            isActive: true,
            participants: 5,
            color: '#3B82F6'
        },
        {
            id: 'project_002',
            name: '모바일 앱 기획',
            description: 'UI/UX 디자인 논의',
            lastMessage: '와이어프레임 검토 완료',
            lastMessageTime: '오후 2:15',
            unreadCount: 0,
            isActive: true,
            participants: 3,
            color: '#10B981'
        },
        {
            id: 'project_003',
            name: '데이터베이스 설계',
            description: '백엔드 아키텍처',
            lastMessage: 'ERD 수정사항 있습니다',
            lastMessageTime: '오전 11:30',
            unreadCount: 1,
            isActive: false,
            participants: 4,
            color: '#8B5CF6'
        },
        {
            id: 'project_004',
            name: '마케팅 전략',
            description: '홍보 및 마케팅',
            lastMessage: '런칭 일정 확정했습니다',
            lastMessageTime: '어제',
            unreadCount: 0,
            isActive: true,
            participants: 6,
            color: '#F59E0B'
        }
    ]);

    // 각 채팅방별 참여자 목록
    const [roomParticipants] = useState({
        'project_001': [
            { id: 'user_001', name: '나', status: 'online', avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=32&h=32&fit=crop&crop=face' },
            { id: 'user_002', name: '김민수', status: 'online', avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=32&h=32&fit=crop&crop=face' },
            { id: 'user_003', name: '이지은', status: 'away', avatar: 'https://images.unsplash.com/photo-1494790108755-2616b612b2c5?w=32&h=32&fit=crop&crop=face' },
            { id: 'user_004', name: '박준호', status: 'online', avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=32&h=32&fit=crop&crop=face' },
            { id: 'user_005', name: '최은영', status: 'offline', avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=32&h=32&fit=crop&crop=face' }
        ],
        'project_002': [
            { id: 'user_001', name: '나', status: 'online', avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=32&h=32&fit=crop&crop=face' },
            { id: 'user_006', name: '정수진', status: 'online', avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=32&h=32&fit=crop&crop=face' },
            { id: 'user_007', name: '강호영', status: 'away', avatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=32&h=32&fit=crop&crop=face' }
        ],
        'project_003': [
            { id: 'user_001', name: '나', status: 'online', avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=32&h=32&fit=crop&crop=face' },
            { id: 'user_008', name: '윤서현', status: 'online', avatar: 'https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?w=32&h=32&fit=crop&crop=face' },
            { id: 'user_009', name: '임재훈', status: 'offline', avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=32&h=32&fit=crop&crop=face' },
            { id: 'user_010', name: '송미영', status: 'online', avatar: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=32&h=32&fit=crop&crop=face' }
        ],
        'project_004': [
            { id: 'user_001', name: '나', status: 'online', avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=32&h=32&fit=crop&crop=face' },
            { id: 'user_011', name: '배현우', status: 'online', avatar: 'https://images.unsplash.com/photo-1519244703995-f4e0f30006d5?w=32&h=32&fit=crop&crop=face' },
            { id: 'user_012', name: '한지원', status: 'online', avatar: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=32&h=32&fit=crop&crop=face' },
            { id: 'user_013', name: '류도현', status: 'away', avatar: 'https://images.unsplash.com/photo-1463453091185-61582044d556?w=32&h=32&fit=crop&crop=face' },
            { id: 'user_014', name: '신예은', status: 'online', avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=32&h=32&fit=crop&crop=face' },
            { id: 'user_015', name: '오민석', status: 'offline', avatar: 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=32&h=32&fit=crop&crop=face' }
        ]
    });

    const messagesEndRef = useRef(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages, currentChatRoom]);

    const handleSendMessage = () => {
        if (newMessage.trim()) {
            const newMsg = {
                id: Date.now(),
                sender: currentUser.name,
                senderId: currentUser.id,
                content: newMessage.trim(),
                time: new Date().toLocaleTimeString('ko-KR', {
                    hour: 'numeric',
                    minute: '2-digit',
                    hour12: true
                }),
                isOwn: true,
                avatar: currentUser.avatar
            };

            setMessages(prev => ({
                ...prev,
                [currentChatRoom]: [...(prev[currentChatRoom] || []), newMsg]
            }));

            setNewMessage('');
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    const handleRoomChange = (roomId) => {
        setCurrentChatRoom(roomId);
    };

    const getCurrentRoom = () => {
        return chatRooms.find(room => room.id === currentChatRoom);
    };

    const getCurrentMessages = () => {
        return messages[currentChatRoom] || [];
    };

    const getCurrentParticipants = () => {
        return roomParticipants[currentChatRoom] || [];
    };

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
                    <h2 style={{ fontSize: '18px', fontWeight: '600', color: '#1F2937', margin: 0 }}>프로젝트 채팅</h2>
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
                        <span style={{ fontSize: '12px', color: '#6B7280' }}>
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
                            <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
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
                                    <Hash style={{ color: 'white' }} />
                                </div>
                                <div style={{ flex: 1, minWidth: 0 }}>
                                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
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
                                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '8px' }}>
                                        <span style={{ fontSize: '11px', color: '#9CA3AF' }}>{room.lastMessageTime}</span>
                                        <div style={{ display: 'flex', alignItems: 'center', fontSize: '11px', color: '#9CA3AF' }}>
                                            <Users style={{ width: '12px', height: '12px', marginRight: '4px' }} />
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
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <div style={{
                            width: '40px',
                            height: '40px',
                            backgroundColor: getCurrentRoom()?.color,
                            borderRadius: '8px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center'
                        }}>
                            <Hash style={{ color: 'white' }} />
                        </div>
                        <div>
                            <h3 style={{ fontSize: '16px', fontWeight: '600', color: '#1F2937', margin: 0 }}>{getCurrentRoom()?.name}</h3>
                            <p style={{ fontSize: '14px', color: '#6B7280', margin: 0 }}>{getCurrentParticipants().length}명 참여 중</p>
                        </div>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
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
                            <Users style={{ color: '#6B7280' }} />
                        </button>
                        <button style={{
                            padding: '8px',
                            borderRadius: '50%',
                            border: 'none',
                            cursor: 'pointer',
                            backgroundColor: 'transparent'
                        }}>
                            <MoreVertical style={{ color: '#6B7280' }} />
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
                                <Hash style={{ color: 'white', width: '32px', height: '32px' }} />
                            </div>
                            <h3 style={{ fontSize: '18px', fontWeight: '500', marginBottom: '8px' }}>{getCurrentRoom()?.name}</h3>
                            <p style={{ fontSize: '14px', textAlign: 'center', lineHeight: '1.5' }}>
                                {getCurrentRoom()?.description}<br />
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
                                            style={{ width: '32px', height: '32px', borderRadius: '50%', flexShrink: 0 }}
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
                                                <p style={{ fontSize: '14px', lineHeight: '1.5', margin: 0 }}>{message.content}</p>
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
                    <div ref={messagesEndRef} />
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
                            <Smile style={{ color: '#6B7280' }} />
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
                            <Send style={{ color: 'inherit' }} />
                        </button>
                    </div>
                </div>
            </div>

            {/* 오른쪽 사이드바 - 현재 채팅방 참여자 */}
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
                            <h3 style={{ fontSize: '16px', fontWeight: '600', color: '#1F2937', margin: 0 }}>참여자</h3>
                            <p style={{ fontSize: '14px', color: '#6B7280', margin: 0 }}>{getCurrentParticipants().length}명</p>
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
                            title="참여자 목록 닫기"
                        >
                            <ChevronRight style={{ color: '#6B7280' }} />
                        </button>
                    </div>

                    <div style={{
                        flex: 1,
                        overflowY: 'auto',
                        padding: '16px'
                    }}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                            {getCurrentParticipants().map((user) => (
                                <div key={user.id} style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '12px',
                                    padding: '8px',
                                    borderRadius: '8px'
                                }}>
                                    <div style={{ position: 'relative' }}>
                                        <img src={user.avatar} alt={user.name} style={{ width: '32px', height: '32px', borderRadius: '50%' }} />
                                        <div style={{
                                            position: 'absolute',
                                            bottom: '-2px',
                                            right: '-2px',
                                            width: '12px',
                                            height: '12px',
                                            borderRadius: '50%',
                                            border: '2px solid white',
                                            backgroundColor: user.status === 'online' ? '#10B981' : user.status === 'away' ? '#F59E0B' : '#6B7280'
                                        }}></div>
                                    </div>
                                    <div style={{ flex: 1 }}>
                                        <p style={{ fontSize: '14px', fontWeight: '500', color: '#1F2937', margin: 0 }}>{user.name}</p>
                                        <p style={{ fontSize: '12px', color: '#6B7280', margin: 0 }}>
                                            {user.status === 'online' ? '온라인' :
                                                user.status === 'away' ? '자리비움' : '오프라인'}
                                        </p>
                                    </div>
                                </div>
                            ))}
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
                        <ChevronLeft style={{ color: '#6B7280' }} />
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