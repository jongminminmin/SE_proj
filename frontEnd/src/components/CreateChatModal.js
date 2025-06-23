import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';

const UserCheck = ({ className }) => <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><polyline points="17 11 19 13 23 9"/></svg>;

export const CreateChatModal = ({ isOpen, onClose, allUsers, onChatRoomCreated }) => {
    const [selectedUserIds, setSelectedUserIds] = useState([]);
    const [step, setStep] = useState('select');
    const [groupName, setGroupName] = useState('');

    useEffect(() => {
        if (!isOpen) {
            setSelectedUserIds([]);
            setStep('select');
            setGroupName('');
        }
    }, [isOpen]);

    const handleUserToggle = (userId) => {
        setSelectedUserIds(prev =>
            prev.includes(userId) ? prev.filter(id => id !== userId) : [...prev, userId]
        );
    };

    const handleCreatePrivateChat = () => {
        if (selectedUserIds.length !== 1) return;
        onChatRoomCreated({ type: 'PRIVATE', user2Id: selectedUserIds[0] });
        onClose();
    };

    const handleCreateGroupChat = () => {
        if (!groupName.trim()) {
            alert("그룹 채팅방 이름을 입력해주세요.");
            return;
        }
        onChatRoomCreated({
            type: 'GROUP',
            name: groupName.trim(),
            participants: selectedUserIds,
        });
        onClose();
    };

    if (!isOpen) return null;

    return createPortal(
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content create-chat-modal" onClick={e => e.stopPropagation()}>
                <div className="modal-header">
                    <h3>{step === 'select' ? '대화 상대 선택' : '그룹 채팅방 정보 입력'}</h3>
                    <button onClick={onClose} className="modal-close-btn">&times;</button>
                </div>
                {step === 'select' ? (
                    <>
                        <div className="modal-body user-select-body">
                            {allUsers.map(user => (
                                <div key={user.id} className={`user-list-item selectable ${selectedUserIds.includes(user.id) ? 'selected' : ''}`} onClick={() => handleUserToggle(user.id)}>
                                    <div className="user-avatar-container"><img src={user.avatar || `https://via.placeholder.com/40/78716C/FFFFFF/?text=${user.username.charAt(0)}`} alt={user.username} className="user-avatar" /></div>
                                    <span className="user-name">{user.username}</span>
                                    <div className="selection-indicator">{selectedUserIds.includes(user.id) && <UserCheck className="check-icon" />}</div>
                                </div>
                            ))}
                        </div>
                        <div className="modal-footer">
                            <button className="modal-action-button private" disabled={selectedUserIds.length !== 1} onClick={handleCreatePrivateChat}>1:1 채팅 시작</button>
                            <button className="modal-action-button group" disabled={selectedUserIds.length === 0} onClick={() => setStep('groupName')}>그룹 채팅 생성</button>
                        </div>
                    </>
                ) : (
                    <>
                        <div className="modal-body">
                            <div className="group-name-input-wrapper">
                                <label htmlFor="groupName">그룹 채팅방 이름</label>
                                <input id="groupName" type="text" value={groupName} onChange={(e) => setGroupName(e.target.value)} placeholder="채팅방 이름을 입력하세요" autoFocus />
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button className="modal-action-button back" onClick={() => setStep('select')}>뒤로</button>
                            <button className="modal-action-button create" disabled={!groupName.trim()} onClick={handleCreateGroupChat}>생성하기</button>
                        </div>
                    </>
                )}
            </div>
        </div>,
        document.body
    );
};
