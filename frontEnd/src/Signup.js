import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import './login.css';

function Signup() {
  const [username, setUsername] = useState('');
  const [id, setId] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  // 비밀번호 유효성 검사 함수
  const validatePassword = (password) => {
    // 최소 8자리 이상, 특수문자 허용
    return password.length >= 8;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // 비밀번호 유효성 검사
    if (!validatePassword(password)) {
      setError('비밀번호는 최소 8자리 이상이어야 합니다.');
      return;
    }

    // 회원가입 데이터 준비
    const data = {
      username, 
      id, 
      password,
      email
    };

    try {
      // API 요청
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(data)
      });

      // 응답 처리
      if (!response.ok) {
        const responseText = await response.text();
        
        // 사용자가 이미 존재하는 경우
        if (response.status === 409 || responseText.includes('already exists')) {
          setError('이미 등록된 사용자입니다.');
        } else {
          setError('회원가입 처리 중 오류가 발생했습니다: ' + responseText);
        }
        return;
      }

      // 성공 시 로그인 페이지로 이동
      alert('회원가입이 완료되었습니다.');
      navigate('/welcome');
      
    } catch (err) {
      console.error('회원가입 요청 오류:', err);
      setError('서버 연결 중 오류가 발생했습니다. 다시 시도해주세요.');
    }
  };

  const handleSocialSignup = (provider) => {
    window.location.href = `/oauth2/authorization/${provider}`;
  };

  return (
    <div className="login-container">
      <h2>회원가입</h2>
      {error && <div className="error-message">{error}</div>}
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="username">이름</label>
          <input
            type="text"
            id="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </div>
        <div className="form-group">
          <label htmlFor="id">아이디</label>
          <input
            type="text"
            id="id"
            value={id}
            onChange={(e) => setId(e.target.value)}
            required
          />
        </div>
        <div className="form-group">
          <label htmlFor="password">비밀번호</label>
          <input
            type="password"
            id="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <small>비밀번호는 8자리 이상이어야 합니다.</small>
        </div>
        <div className="form-group">
          <label htmlFor="email">이메일</label>
          <input
            type="email"
            id="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>
        <button type="submit">회원가입</button>
      </form>
      <div className="social-login-buttons">
        <button 
          className="kakao-btn" 
          type="button" 
          onClick={() => handleSocialSignup('kakao')}
        >
          카카오로 회원가입
        </button>
        <button 
          className="google-btn" 
          type="button" 
          onClick={() => handleSocialSignup('google')}
        >
          구글로 회원가입
        </button>
      </div>
      <div className="signup-link">
        이미 계정이 있으신가요? <Link to="/login">로그인</Link>
      </div>
    </div>
  );
}

export default Signup;