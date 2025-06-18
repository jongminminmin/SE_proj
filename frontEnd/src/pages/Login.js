import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Login.css';
const googleLogo = 'https://developers.google.com/identity/images/g-logo.png';

function Login() {
    const navigate = useNavigate();
    // 로그인 폼 상태
    const [userId, setUserId] = useState('');
    const [password, setPassword] = useState('');
    const [loginError, setLoginError] = useState(false); // 로그인 오류 상태명 변경 (error -> loginError)

    // --- 아이디 찾기 로직 추가 시작 (현재 Login.js UI에는 없으므로 관련 상태 및 핸들러 제거) ---
    // const [findIdEmail, setFindIdEmail] = useState('');
    // const [findIdResult, setFindIdResult] = useState('');
    // const [findIdError, setFindIdError] = useState('');
    // const handleFindIdEmailChange = (e) => { setFindIdEmail(e.target.value); };
    // const handleFindIdSubmit = async (event) => { /* ... */ };
    // --- 아이디 찾기 로직 추가 끝 ---

    // --- 비밀번호 찾기(재설정 전 사용자 확인) 로직 추가 시작 (현재 Login.js UI에는 없으므로 관련 상태 및 핸들러 제거) ---
    // const [findPasswordForm, setFindPasswordForm] = useState({
    //     userId: '',
    //     email: '',
    // });
    // const [findPasswordResult, setFindPasswordResult] = useState('');
    // const [findPasswordError, setFindPasswordError] = useState('');
    // const handleFindPasswordChange = (e) => { /* ... */ };
    // const handleFindPasswordSubmit = async (event) => { /* ... */ };
    // --- 비밀번호 찾기 로직 추가 끝 ---

    const handleLogin = async (e) => {
        e.preventDefault();
        setLoginError(false);
        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId, password }),
                credentials: 'include'
            });

            if (!response.ok) {
                setLoginError(true);
                return;
            }
            const data = await response.json();
            if (data.token) {
                localStorage.setItem('token', data.token);
            }
            navigate('/main');
        } catch (error) {
            setLoginError(true);
        }
    };

    const goToRegister = () => {
        navigate('/register');
    };

    const goToFindAccount = () => {
        navigate('/find-account');
    };

    // 구글 소셜 로그인 핸들러
    const handleGoogleLogin = () => {
        window.location.href = '/oauth2/authorization/google';
    };

    return (
        <div className="login-wrapper">
            <div className="mate-logo">MATE</div>
            <div className="login-left">
                <h1 className="main-title">함께 만드는 프로젝트,<br />당신의 완벽한 파트너.</h1>
                <p className="main-desc">Mate와 함께라면 협업이 더 쉬워집니다.<br />지금 바로 시작해보세요!</p>
            </div>
            <div className="login-right">
                <form className="login-form" onSubmit={handleLogin}>
                    {loginError && (
                        <div className="error-message">
                            아이디 또는 비밀번호가 올바르지 않습니다.
                        </div>
                    )}
                    <input
                        type="text"
                        name="userId"
                        placeholder="아이디"
                        value={userId}
                        onChange={e => setUserId(e.target.value)}
                        className="login-input"
                        required
                    />
                    <input
                        type="password"
                        placeholder="비밀번호"
                        name="password"
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        className="login-input"
                        required
                    />
                    <button type="submit" className="login-btn">로그인</button>
                    <button type="button" className="google-login-btn" onClick={handleGoogleLogin}>
                        <img src={googleLogo} alt="Google Logo" className="google-logo" />
                        Google로 로그인
                    </button>
                    <div className="find-links">
                        <span onClick={goToFindAccount} className="link-text">아이디/비밀번호 찾기</span>
                    </div>
                    <div className="register-link">
                        계정이 없으신가요?{' '}
                        <span onClick={goToRegister} className="link-text">회원가입</span>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default Login;
