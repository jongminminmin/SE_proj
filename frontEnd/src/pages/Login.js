import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom'; // useLocation은 현재 사용되지 않으므로 제거 가능
import './Login.css';

function Login() {
    const navigate = useNavigate();
    // 로그인 폼 상태
    const [userId, setUserId] = useState('');
    const [password, setPassword] = useState('');
    const [loginError, setLoginError] = useState(false); // 로그인 오류 상태명 변경 (error -> loginError)

    // --- 아이디 찾기 로직 추가 시작 ---
    const [findIdEmail, setFindIdEmail] = useState(''); // 아이디 찾기 시 입력할 이메일 상태
    const [findIdResult, setFindIdResult] = useState(''); // 아이디 찾기 결과 메시지 또는 찾은 아이디
    const [findIdError, setFindIdError] = useState('');   // 아이디 찾기 오류 메시지

    // 아이디 찾기 입력 변경 핸들러 (이 UI는 현재 Login.js에 없습니다)
    const handleFindIdEmailChange = (e) => {
        setFindIdEmail(e.target.value);
    };

    // 아이디 찾기 제출 핸들러 (API 호출 로직)
    // 이 함수는 별도의 아이디 찾기 UI에서 호출되어야 합니다.
    const handleFindIdSubmit = async (event) => {
        event.preventDefault();
        setFindIdResult('');
        setFindIdError('');

        if (!findIdEmail) {
            setFindIdError('이메일을 입력해주세요.');
            return;
        }

        try {
            const response = await fetch('/api/auth/find/id', { // 서버의 아이디 찾기 API 엔드포인트
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ email: findIdEmail }),
            });

            const data = await response.json();

            if (response.ok && data.success) {
                setFindIdResult(`찾으시는 아이디는 [${data.userId}] 입니다.`); // 실제 운영 시 아이디 전체 노출 주의
            } else {
                setFindIdError(data.message || '해당 이메일로 가입된 아이디를 찾을 수 없습니다.');
                console.error('Find ID failed:', data.message);
            }
        } catch (err) {
            setFindIdError('네트워크 오류 또는 서버에 연결할 수 없습니다.');
            console.error('Network error or server issue during find ID:', err);
        }
    };
    // --- 아이디 찾기 로직 추가 끝 ---

    // --- 비밀번호 찾기(재설정 전 사용자 확인) 로직 추가 시작 ---
    const [findPasswordForm, setFindPasswordForm] = useState({
        userId: '',
        email: '',
    });
    const [findPasswordResult, setFindPasswordResult] = useState('');
    const [findPasswordError, setFindPasswordError] = useState('');

    // 비밀번호 찾기 폼 입력 변경 핸들러 (이 UI는 현재 Login.js에 없습니다)
    const handleFindPasswordChange = (e) => {
        setFindPasswordForm({ ...findPasswordForm, [e.target.name]: e.target.value });
    };

    // 비밀번호 찾기 제출 핸들러 (API 호출 로직)
    // 이 함수는 별도의 비밀번호 찾기 UI에서 호출되어야 합니다.
    const handleFindPasswordSubmit = async (event) => {
        event.preventDefault();
        setFindPasswordResult('');
        setFindPasswordError('');

        if (!findPasswordForm.userId || !findPasswordForm.email) {
            setFindPasswordError('아이디와 이메일을 모두 입력해주세요.');
            return;
        }

        try {
            const response = await fetch('/api/auth/find/password', { // 서버의 비밀번호 찾기 API 엔드포인트
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(findPasswordForm),
            });

            const data = await response.json();

            if (response.ok && data.success) {
                // 실제 비밀번호를 반환하지 않고, 재설정 절차 안내 메시지를 표시
                setFindPasswordResult(data.message || '비밀번호 재설정 안내 메일을 발송했습니다. (실제 발송 기능은 구현 필요)');
            } else {
                setFindPasswordError(data.message || '입력하신 정보와 일치하는 사용자를 찾을 수 없습니다.');
                console.error('Find Password failed:', data.message);
            }
        } catch (err) {
            setFindPasswordError('네트워크 오류 또는 서버에 연결할 수 없습니다.');
            console.error('Network error or server issue during find password:', err);
        }
    };
    // --- 비밀번호 찾기 로직 추가 끝 ---

    const handleLogin = async (e) => {
        e.preventDefault();
        setLoginError(false); // 이전 로그인 오류 메시지 초기화
        try {
            const response = await fetch('/api/auth/login', { // 로그인 API 경로
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userId: userId,
                    password: password
                }),
                credentials: 'include' // 쿠키/세션 정보 포함
            });

            if (!response.ok) {
                let errorData;
                try {
                    errorData = await response.json();
                } catch (jsonError) {
                    const errorText = await response.text();
                    throw new Error(errorText || `로그인 실패 (상태 코드: ${response.status})`);
                }
                throw new Error(errorData.message || '로그인에 실패했습니다.');
            }

            const data = await response.json();
            if (data.token) {
                localStorage.setItem('token', data.token);
            }
            navigate('/main');

        } catch (error) {
            setLoginError(true);
            console.error('로그인 오류:', error.message);
        }
    };

    const goToRegister = () => {
        navigate('/register');
    };

    return (
        <div className="login-container">
            <form className="login-form" onSubmit={handleLogin}>
                <h2 className="login-title">로그인</h2>

                {loginError && ( // 로그인 오류 메시지만 표시
                    <div className="error-message">
                        아이디 또는 비밀번호가 올바르지 않습니다.
                    </div>
                )}

                {/* 아이디 찾기 결과/오류 메시지 표시는 현재 로그인 UI에 포함되지 않음 */}
                {/* {findIdError && <p className="error-message">{findIdError}</p>} */}
                {/* {findIdResult && <p className="success-message">{findIdResult}</p>} */}

                {/* 비밀번호 찾기 결과/오류 메시지 표시는 현재 로그인 UI에 포함되지 않음 */}
                {/* {findPasswordError && <p className="error-message">{findPasswordError}</p>} */}
                {/* {findPasswordResult && <p className="success-message">{findPasswordResult}</p>} */}

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
                <div className="register-link">
                    계정이 없으신가요?{' '}
                    <span onClick={goToRegister}>회원가입</span>
                </div>
                {/* 아이디/비밀번호 찾기 링크 또는 버튼을 여기에 추가하고,
                    클릭 시 해당 UI(모달 등)를 보여주고,
                    그 UI의 폼에서 handleFindIdSubmit 또는 handleFindPasswordSubmit 함수를 호출하도록 구현해야 합니다.
                    예:
                    <div className="find-links">
                        <span onClick={보여줄_아이디찾기_UI_함수}>아이디 찾기</span>
                        {' / '}
                        <span onClick={보여줄_비밀번호찾기_UI_함수}>비밀번호 찾기</span>
                    </div>
                */}
            </form>
        </div>
    );
}

export default Login;
