import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from "../App"; // AuthProvider가 정의된 App.js 또는 AuthContext.js 파일 경로를 정확히 지정해야 합니다.
import './Login.css';

function Login() {
    const navigate = useNavigate();
    const { login: authContextLogin } = useAuth(); // AuthContext의 login 함수 사용

    const [userId, setUserId] = useState('');
    const [password, setPassword] = useState('');
    const [loginError, setLoginError] = useState(''); // 오류 메시지를 문자열로 저장

    // --- 아이디 찾기 로직 (기존 코드 유지) ---
    const [findIdEmail, setFindIdEmail] = useState('');
    const [findIdResult, setFindIdResult] = useState('');
    const [findIdError, setFindIdError] = useState('');

    const handleFindIdEmailChange = (e) => setFindIdEmail(e.target.value);

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
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: findIdEmail }),
            });
            const data = await response.json();
            if (response.ok && data.success) {
                setFindIdResult(`찾으시는 아이디는 [${data.userId}] 입니다.`);
            } else {
                setFindIdError(data.message || '해당 이메일로 가입된 아이디를 찾을 수 없습니다.');
                console.error('Find ID failed:', data.message);
            }
        } catch (err) {
            setFindIdError('네트워크 오류 또는 서버에 연결할 수 없습니다.');
            console.error('Network error or server issue during find ID:', err);
        }
    };
    // --- 아이디 찾기 로직 끝 ---

    // --- 비밀번호 찾기 로직 (기존 코드 유지) ---
    const [findPasswordForm, setFindPasswordForm] = useState({ userId: '', email: '' });
    const [findPasswordResult, setFindPasswordResult] = useState('');
    const [findPasswordError, setFindPasswordError] = useState('');

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
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(findPasswordForm),
            });
            const data = await response.json();
            if (response.ok && data.success) {
                setFindPasswordResult(data.message || '비밀번호 재설정 안내 메일을 발송했습니다.');
            } else {
                setFindPasswordError(data.message || '입력하신 정보와 일치하는 사용자를 찾을 수 없습니다.');
                console.error('Find Password failed:', data.message);
            }
        } catch (err) {
            setFindPasswordError('네트워크 오류 또는 서버에 연결할 수 없습니다.');
            console.error('Network error or server issue during find password:', err);
        }
    };
    // --- 비밀번호 찾기 로직 끝 ---

    const handleLogin = async (e) => {
        e.preventDefault();
        setLoginError('');
        console.log('Attempting login with:', { userId, password });

        try {
            // 1. 로그인 API 호출
            const loginApiResponse = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userId: userId,
                    password: password
                }),
                credentials: 'include' // 세션 쿠키 교환을 위해 필수
            });

            const responseText = await loginApiResponse.text(); // 먼저 텍스트로 응답을 받음
            console.log('Raw login API response text:', responseText);

            let loginApiData;
            try {
                loginApiData = JSON.parse(responseText); // 텍스트를 JSON으로 파싱 시도
            } catch (jsonError) {
                console.error('Login API response was not JSON. Status:', loginApiResponse.status, ". Body:", responseText, jsonError);
                throw new Error(`로그인 서버 응답 형식 오류입니다. (상태 코드: ${loginApiResponse.status})`);
            }

            console.log('Login API response data:', loginApiData);

            if (!loginApiResponse.ok || !loginApiData.success) {
                throw new Error(loginApiData.message || '아이디 또는 비밀번호가 올바르지 않습니다.');
            }

            // 2. 로그인 API 성공 시, /api/users/me를 호출하여 사용자 상세 정보 가져오기
            console.log('Login API successful, fetching user details from /api/users/me');
            const userDetailsResponse = await fetch('/api/users/me', {
                credentials: 'include' // 세션 쿠키 전송
            });

            const userDetailsResponseText = await userDetailsResponse.text();
            console.log('Raw /api/users/me response text:', userDetailsResponseText);

            let userDataFromApi;
            try {
                userDataFromApi = JSON.parse(userDetailsResponseText);
            } catch (jsonError) {
                console.error('/api/users/me response was not JSON. Status:', userDetailsResponse.status, ". Body:", userDetailsResponseText, jsonError);
                // 이 오류는 /api/auth/login 후 세션 쿠키가 제대로 설정되지 않아 /api/users/me가 HTML(로그인 페이지)을 반환할 때 주로 발생합니다.
                throw new Error(`사용자 정보를 가져오는 중 예상치 못한 응답을 받았습니다. (상태 코드: ${userDetailsResponse.status})`);
            }

            if (!userDetailsResponse.ok) {
                // UserDto가 정상적으로 왔지만, 혹시 모를 다른 HTTP 오류 케이스
                throw new Error(userDataFromApi.message || `로그인 후 사용자 정보를 가져오는 데 실패했습니다. (상태: ${userDetailsResponse.status})`);
            }

            console.log('Fetched user details:', userDataFromApi);

            // 3. AuthProvider의 login 함수를 호출하여 전역 상태 업데이트
            const appUser = {
                id: userDataFromApi.id,
                name: userDataFromApi.username,
                avatar: userDataFromApi.avatarUrl || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=40&h=40&fit=crop&crop=face'
            };
            authContextLogin(appUser);
            console.log('Global currentUser state updated via AuthContext.');

            // 4. 메인 페이지로 이동
            console.log('Navigating to /main');
            navigate('/main');

        } catch (error) {
            console.error('로그인 처리 중 전체 오류:', error.message, error);
            setLoginError(error.message || '로그인 중 알 수 없는 오류가 발생했습니다.');
        }
    };

    const goToRegister = () => {
        navigate('/register');
    };

    const goToFindAccount = () => {
        navigate('/find-account');
    };
   //구글 소셜로그인
    const handleGoogleLogin = () => {
        // 스프링 시큐리티의 OAuth2 로그인 엔드포인트로 리디렉션
        window.location.href = '/oauth2/authorization/google';
    };


    return (
        <div className="landing-login-container">
            <div className="mate-logo">MATE</div>

            <div className="landing-left">
                <h1 className="main-title">
                    함께 만드는 프로젝트,<br />
                    당신의 완벽한 파트너.
                </h1>
            </div>

            <div className="login-section">
                <form className="login-form" onSubmit={handleLogin}>
                    {loginError && (
                        <div className="error-message">
                            {loginError}
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
                    <div className="divider">
                        <span>또는</span>
                    </div>

                    <button type="button" className="google-login-btn" onClick={handleGoogleLogin}>
                        <svg className="google-icon" viewBox="0 0 24 24" width="20" height="20">
                            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                        </svg>
                        Google로 로그인
                    </button>
                    <div className="auth-links">
                        <span onClick={goToFindAccount}>아이디/비밀번호 찾기</span>
                        <span onClick={goToRegister}>회원가입</span>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default Login;