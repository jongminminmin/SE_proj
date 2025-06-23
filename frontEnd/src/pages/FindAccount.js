import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './FindAccount.css';

function FindAccount() {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('findId'); // 'findId' or 'findPassword'

    // 아이디 찾기 상태
    const [findIdEmail, setFindIdEmail] = useState('');
    const [findIdResult, setFindIdResult] = useState('');
    const [findIdError, setFindIdError] = useState('');

    // 비밀번호 찾기 상태
    const [findPasswordForm, setFindPasswordForm] = useState({
        userId: '',
        email: '',
    });
    const [findPasswordError, setFindPasswordError] = useState('');
    const [isUserVerified, setIsUserVerified] = useState(false); // 사용자 확인 여부
    const [newPassword, setNewPassword] = useState('');
    const [confirmNewPassword, setConfirmNewPassword] = useState('');
    const [resetResultMessage, setResetResultMessage] = useState('');

    // 아이디 찾기 핸들러
    const handleFindIdSubmit = async (event) => {
        event.preventDefault();
        setFindIdResult('');
        setFindIdError('');

        if (!findIdEmail) {
            setFindIdError('이메일을 입력해주세요.');
            return;
        }

        try {
            const response = await fetch('/api/auth/find/id', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ email: findIdEmail }),
            });

            const data = await response.json();

            if (response.ok && data.success) {
                setFindIdResult(`찾으시는 아이디는 [${data.userId}] 입니다.`);
            } else {
                setFindIdError(data.message || '해당 이메일로 가입된 아이디를 찾을 수 없습니다.');
            }
        } catch (err) {
            setFindIdError('네트워크 오류 또는 서버에 연결할 수 없습니다.');
        }
    };

    // 비밀번호 찾기 - 사용자 확인 핸들러
    const handleVerifyUserSubmit = async (event) => {
        event.preventDefault();
        setFindPasswordError('');
        setResetResultMessage('');

        if (!findPasswordForm.userId || !findPasswordForm.email) {
            setFindPasswordError('아이디와 이메일을 모두 입력해주세요.');
            return;
        }

        try {
            const response = await fetch('/api/auth/find/password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(findPasswordForm),
            });
            const data = await response.json();
            if (response.ok && data.success) {
                setIsUserVerified(true); // 사용자 확인 성공
            } else {
                setFindPasswordError(data.message || '입력하신 정보와 일치하는 사용자를 찾을 수 없습니다.');
            }
        } catch (err) {
            setFindPasswordError('네트워크 오류 또는 서버에 연결할 수 없습니다.');
        }
    };

    // 비밀번호 찾기 - 새 비밀번호 설정 핸들러
    const handleResetPasswordSubmit = async (event) => {
        event.preventDefault();
        setFindPasswordError('');
        setResetResultMessage('');

        if (newPassword !== confirmNewPassword) {
            setFindPasswordError('새 비밀번호가 일치하지 않습니다.');
            return;
        }
        if (newPassword.length < 6) {
            setFindPasswordError('비밀번호는 6자 이상이어야 합니다.');
            return;
        }

        try {
            const response = await fetch('/api/auth/reset-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    userId: findPasswordForm.userId,
                    email: findPasswordForm.email,
                    newPassword: newPassword,
                }),
            });
            const data = await response.json();
            if (response.ok && data.success) {
                setResetResultMessage('비밀번호가 성공적으로 변경되었습니다. 3초 후 로그인 페이지로 이동합니다.');
                setTimeout(() => navigate('/login'), 3000);
            } else {
                setFindPasswordError(data.message || '비밀번호 변경에 실패했습니다.');
            }
        } catch (err) {
            setFindPasswordError('네트워크 오류 또는 서버에 연결할 수 없습니다.');
        }
    };

    const goToLogin = () => {
        navigate('/login');
    };

    return (
        <div className="find-account-container">
            <div className="find-account-box">
                <h2 className="find-account-title">계정 찾기</h2>

                {/* 탭 메뉴 */}
                <div className="tab-menu">
                    <button
                        className={`tab-button ${activeTab === 'findId' ? 'active' : ''}`}
                        onClick={() => setActiveTab('findId')}
                    >
                        아이디 찾기
                    </button>
                    <button
                        className={`tab-button ${activeTab === 'findPassword' ? 'active' : ''}`}
                        onClick={() => setActiveTab('findPassword')}
                    >
                        비밀번호 찾기
                    </button>
                </div>

                {/* 아이디 찾기 탭 */}
                {activeTab === 'findId' && (
                    <div className="tab-content">
                        <form onSubmit={handleFindIdSubmit}>
                            <p className="form-description">
                                회원가입 시 등록한 이메일 주소를 입력해주세요.
                            </p>
                            <input
                                type="email"
                                placeholder="이메일 주소"
                                value={findIdEmail}
                                onChange={(e) => setFindIdEmail(e.target.value)}
                                className="find-input"
                                required
                            />
                            {findIdError && <div className="error-message">{findIdError}</div>}
                            {findIdResult && <div className="success-message">{findIdResult}</div>}
                            <button type="submit" className="find-submit-btn">아이디 찾기</button>
                        </form>
                    </div>
                )}

                {/* 비밀번호 찾기 탭 */}
                {activeTab === 'findPassword' && (
                    <div className="tab-content">
                        {!isUserVerified ? (
                            <form onSubmit={handleVerifyUserSubmit}>
                                <p className="form-description">
                                    아이디와 회원가입 시 등록한 이메일 주소를 입력해주세요.
                                </p>
                                <input
                                    type="text"
                                    name="userId"
                                    placeholder="아이디"
                                    value={findPasswordForm.userId}
                                    onChange={(e) => setFindPasswordForm({ ...findPasswordForm, userId: e.target.value })}
                                    className="find-input"
                                    required
                                />
                                <input
                                    type="email"
                                    name="email"
                                    placeholder="이메일 주소"
                                    value={findPasswordForm.email}
                                    onChange={(e) => setFindPasswordForm({ ...findPasswordForm, email: e.target.value })}
                                    className="find-input"
                                    required
                                />
                                {findPasswordError && <div className="error-message">{findPasswordError}</div>}
                                <button type="submit" className="find-submit-btn">사용자 확인</button>
                            </form>
                        ) : (
                            <form onSubmit={handleResetPasswordSubmit}>
                                <p className="form-description">새로운 비밀번호를 입력해주세요.</p>
                                <input
                                    type="password"
                                    placeholder="새 비밀번호"
                                    value={newPassword}
                                    onChange={(e) => setNewPassword(e.target.value)}
                                    className="find-input"
                                    required
                                />
                                <input
                                    type="password"
                                    placeholder="새 비밀번호 확인"
                                    value={confirmNewPassword}
                                    onChange={(e) => setConfirmNewPassword(e.target.value)}
                                    className="find-input"
                                    required
                                />
                                {findPasswordError && <div className="error-message">{findPasswordError}</div>}
                                {resetResultMessage && <div className="success-message">{resetResultMessage}</div>}
                                <button type="submit" className="find-submit-btn">비밀번호 변경</button>
                            </form>
                        )}
                    </div>
                )}

                <div className="back-to-login">
                    <button onClick={goToLogin} className="back-btn">로그인으로 돌아가기</button>
                </div>
            </div>
        </div>
    );
}

export default FindAccount;