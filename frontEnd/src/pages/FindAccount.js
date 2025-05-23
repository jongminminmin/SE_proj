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
    const [findPasswordResult, setFindPasswordResult] = useState('');
    const [findPasswordError, setFindPasswordError] = useState('');

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

    // 비밀번호 찾기 핸들러
    const handleFindPasswordChange = (e) => {
        setFindPasswordForm({ ...findPasswordForm, [e.target.name]: e.target.value });
    };

    const handleFindPasswordSubmit = async (event) => {
        event.preventDefault();
        setFindPasswordResult('');
        setFindPasswordError('');

        if (!findPasswordForm.userId || !findPasswordForm.email) {
            setFindPasswordError('아이디와 이메일을 모두 입력해주세요.');
            return;
        }

        try {
            const response = await fetch('/api/auth/find/password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(findPasswordForm),
            });

            const data = await response.json();

            if (response.ok && data.success) {
                setFindPasswordResult(data.message || '비밀번호 재설정 안내 메일을 발송했습니다.');
            } else {
                setFindPasswordError(data.message || '입력하신 정보와 일치하는 사용자를 찾을 수 없습니다.');
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
                        <form onSubmit={handleFindPasswordSubmit}>
                            <p className="form-description">
                                아이디와 회원가입 시 등록한 이메일 주소를 입력해주세요.
                            </p>
                            <input
                                type="text"
                                name="userId"
                                placeholder="아이디"
                                value={findPasswordForm.userId}
                                onChange={handleFindPasswordChange}
                                className="find-input"
                                required
                            />
                            <input
                                type="email"
                                name="email"
                                placeholder="이메일 주소"
                                value={findPasswordForm.email}
                                onChange={handleFindPasswordChange}
                                className="find-input"
                                required
                            />
                            {findPasswordError && <div className="error-message">{findPasswordError}</div>}
                            {findPasswordResult && <div className="success-message">{findPasswordResult}</div>}
                            <button type="submit" className="find-submit-btn">비밀번호 찾기</button>
                        </form>
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