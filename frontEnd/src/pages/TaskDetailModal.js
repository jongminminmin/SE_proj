import React, { useState, useEffect, useCallback } from 'react';
import { X, Send, Edit, Trash2 } from 'lucide-react';
import defaultProfile from '../assets/default-profile.png';
import './TaskDetailModal.css';

const TaskDetailModal = ({ task, onClose, onUpdateTask, currentUser }) => {
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState('');
    const [editingCommentId, setEditingCommentId] = useState(null);
    const [editingContent, setEditingContent] = useState('');

    const fetchComments = useCallback(async () => {
        if (!task) return;
        try {
            const response = await fetch(`/api/tasks/${task.taskNo}/comments`);
            if (response.ok) {
                const data = await response.json();
                setComments(data);
            }
        } catch (error) {
            console.error("Failed to fetch comments:", error);
        }
    }, [task]);

    useEffect(() => {
        fetchComments();
    }, [fetchComments]);

    const handleAddComment = async () => {
        if (!newComment.trim()) return;
        try {
            const response = await fetch(`/api/tasks/${task.taskNo}/comments`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content: newComment }),
            });
            if (response.ok) {
                setNewComment('');
                fetchComments(); 
                if (onUpdateTask) onUpdateTask();
            }
        } catch (error) {
            console.error("Failed to add comment:", error);
        }
    };

    const handleEditClick = (comment) => {
        setEditingCommentId(comment.id);
        setEditingContent(comment.content);
    };

    const handleCancelEdit = () => {
        setEditingCommentId(null);
        setEditingContent('');
    };

    const handleUpdateComment = async () => {
        if (!editingContent.trim()) return;
        try {
            const response = await fetch(`/api/tasks/${task.taskNo}/comments/${editingCommentId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content: editingContent }),
            });
            if (response.ok) {
                handleCancelEdit();
                fetchComments();
                if (onUpdateTask) onUpdateTask();
            }
        } catch (error) {
            console.error("Failed to update comment:", error);
        }
    };

    const handleDeleteComment = async (commentId) => {
        if (window.confirm('정말로 이 댓글을 삭제하시겠습니까?')) {
            try {
                const response = await fetch(`/api/tasks/${task.taskNo}/comments/${commentId}`, {
                    method: 'DELETE',
                });
                if (response.ok) {
                    fetchComments();
                    if (onUpdateTask) onUpdateTask();
                }
            } catch (error) {
                console.error("Failed to delete comment:", error);
            }
        }
    };

    if (!task) return null;

    return (
        <div className="detail-modal-overlay">
            <div className="detail-modal">
                <button onClick={onClose} className="close-button"><X size={24} /></button>
                <div className="modal-content">
                    <div className="task-details-main">
                        <h2 className="task-title-in-modal">{task.taskTitle}</h2>
                        <p className="task-description-in-modal">{task.description}</p>
                        
                        <div className="comments-section">
                            <h3 className="comments-title">활동</h3>
                            <div className="comment-input-area">
                                <img src={defaultProfile} alt="My Profile" className="my-profile-pic" />
                                <div className="comment-input-wrapper">
                                    <input
                                        type="text"
                                        value={newComment}
                                        onChange={(e) => setNewComment(e.target.value)}
                                        placeholder="댓글 추가..."
                                        onKeyPress={(e) => e.key === 'Enter' && handleAddComment()}
                                    />
                                    <button onClick={handleAddComment}><Send size={16} /></button>
                                </div>
                            </div>
                            <div className="comments-list">
                                {comments.map(comment => (
                                    <div key={comment.id} className="comment-item">
                                        <img 
                                            src={defaultProfile}
                                            alt={comment.author.username} 
                                            className="comment-author-pic" 
                                        />
                                        <div className="comment-content">
                                            <div className="comment-header">
                                                <span className="comment-author-name">{comment.author.username}</span>
                                                <span className="comment-timestamp">{new Date(comment.createdAt).toLocaleString()}</span>
                                                {currentUser && currentUser.username === comment.author.username && (
                                                    <div className="comment-actions">
                                                        <button onClick={() => handleEditClick(comment)} className="comment-action-btn">수정</button>
                                                        <button onClick={() => handleDeleteComment(comment.id)} className="comment-action-btn">삭제</button>
                                                    </div>
                                                )}
                                            </div>
                                            {editingCommentId === comment.id ? (
                                                <div className="comment-edit-area">
                                                    <input
                                                        type="text"
                                                        value={editingContent}
                                                        onChange={(e) => setEditingContent(e.target.value)}
                                                        className="comment-edit-input"
                                                    />
                                                    <div className="comment-edit-buttons">
                                                        <button onClick={handleUpdateComment} className="btn-save">저장</button>
                                                        <button onClick={handleCancelEdit} className="btn-cancel">취소</button>
                                                    </div>
                                                </div>
                                            ) : (
                                                <p className="comment-text">{comment.content}</p>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default TaskDetailModal; 