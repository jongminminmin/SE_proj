import React, { useState } from 'react';
import './Register.css';
import {useNavigate} from "react-router-dom";


function Register() {
  const [form, setForm] = useState({
    id: '',
    username: '',
    password: '',
    email: '',
  });

  const [error, setError] = useState('')//오류 메시지 상태
  const [success, setSuccess] = useState('')//성공 메시지 상태
  const navigate = useNavigate(); //React-Router의 navigate 함수 사용

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    // --- 비밀번호 유효성 검사 로직 추가 ---
    const passwordRegex = /(?=.*[\W_])/; // 특수문자 또는 밑줄(_) 포함 여부
    if (form.password.length < 8) {
      setError('비밀번호는 최소 8자 이상이어야 합니다.');
      return;
    }
    if (!passwordRegex.test(form.password)) {
      setError('비밀번호에는 특수문자가 최소 하나 이상 포함되어야 합니다.');
      return;
    }
    // --- 유효성 검사 로직 끝 ---

    try {
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(form),
      });

      if (response.ok) {
        //HTTP 상태 코드가 2XX인 경우
        const resultText = await response.text(); //서버에서 가입성공 같읕 메시지 리턴한다고 가정
        setSuccess(resultText || '가입 성공')
        //0.5초후 로그인 페이지로 이동
        setTimeout(() => {
          navigate('/login'); // React Router를 사용한 페이지 이동
        }, 500);
      } else {
        // HTTP 상태 코드가 2xx가 아닌 경우 (실패)
        const errorText = await response.text(); // 서버에서 오류 메시지를 텍스트로 반환한다고 가정
        setError(errorText || '회원가입에 실패했습니다. 입력 정보를 확인해주세요.');
        console.error('Registration failed:', errorText);
      }
    }
    catch (err) {
        setError('네트워크 오류 또는 서버에 연결할 수 없습니다.');
        console.error('Network error or server issue:', err);
      }
  };

  const handleLoginLinkClick = () => {
    navigate('/login'); // React Router를 사용한 페이지 이동
  };

  // return (
  //   <div className="register-container">
  //     <form className="register-form" onSubmit={handleSubmit}>
  //       <h2 className="register-title">회원가입</h2>
  //       <input
  //         type="text"
  //         name="id"
  //         placeholder="아이디"
  //         value={form.id}
  //         onChange={handleChange}
  //         className="register-input"
  //         required
  //       />
  //       <input
  //         type="text"
  //         name="username"
  //         placeholder="사용자명"
  //         value={form.username}
  //         onChange={handleChange}
  //         className="register-input"
  //         required
  //       />
  //       <input
  //         type="password"
  //         name="password"
  //         placeholder="비밀번호"
  //         value={form.password}
  //         onChange={handleChange}
  //         className="register-input"
  //         required
  //       />
  //       <input
  //         type="email"
  //         name="email"
  //         placeholder="이메일"
  //         value={form.email}
  //         onChange={handleChange}
  //         className="register-input"
  //         required
  //       />
  //       <button type="submit" className="register-btn">회원가입</button>
  //       <div className="login-link">
  //         이미 계정이 있으신가요?{' '}
  //         <span onClick={() => window.location.href='/login'}>로그인</span>
  //       </div>
  //     </form>
  //   </div>
  // );

  return (
      <div className="register-container">
        <form className="register-form" onSubmit={handleSubmit}>
          <h2 className="register-title">회원가입</h2>
          {error && <p className="error-message">{error}</p>}
          {success && <p className="success-message">{success}</p>}
          <input
              type="text"
              name="id"
              placeholder="아이디"
              value={form.id}
              onChange={handleChange}
              className="register-input"
              required
          />
          <input
              type="text"
              name="username"
              placeholder="사용자명"
              value={form.username}
              onChange={handleChange}
              className="register-input"
              required
          />
          <input
              type="password"
              name="password"
              placeholder="비밀번호"
              value={form.password}
              onChange={handleChange}
              className="register-input"
              required
          />
          <input
              type="email"
              name="email"
              placeholder="이메일"
              value={form.email}
              onChange={handleChange}
              className="register-input"
              required
          />
          <button type="submit" className="register-btn">회원가입</button>
          <div className="login-link-container"> {
            /* login-link에서 login-link-container로 변경 (스타일 충돌 방지) */}
            이미 계정이 있으신가요?{' '}
            {/* React Router의 Link 컴포넌트 사용을 권장하지만, 여기서는 navigate 함수 사용 */}
            <span onClick={handleLoginLinkClick} className="login-navigation-link">로그인</span>
          </div>
        </form>
      </div>
  );
}

export default Register; 