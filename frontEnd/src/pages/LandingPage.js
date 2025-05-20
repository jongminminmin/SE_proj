import React from 'react';
import { useNavigate } from 'react-router-dom';
import './LandingPage.css';

function LandingPage() {
  const navigate = useNavigate();

  const handleStart = () => {
    navigate('/login');
  };

  return (
    <div className="landing-container">
      <div className="mate-logo">MATE</div>
      <div className="landing-left">
        <h1 className="main-title">
          함께 만드는 프로젝트,<br />
          당신의 완벽한 파트너.
        </h1>
        <span className="start-link" onClick={handleStart}>
          <span style={{fontSize: '1.2em', marginRight: '0.4em', verticalAlign: '-2px'}}>→</span>
          시작하기
        </span>
      </div>
    </div>
  );
}

export default LandingPage; 