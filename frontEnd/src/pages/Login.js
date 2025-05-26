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

    return (
        <div className="login-container">
            <form className="login-form" onSubmit={handleLogin}>
                <h2 className="login-title">로그인</h2>
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
                <div className="find-links">
                    <span onClick={goToFindAccount} style={{cursor: 'pointer'}}>아이디/비밀번호 찾기</span>
                </div>
                <div className="register-link">
                    계정이 없으신가요?{' '}
                    <span onClick={goToRegister} style={{cursor: 'pointer'}}>회원가입</span>
                </div>
            </form>
        </div>
    );
}

export default Login;