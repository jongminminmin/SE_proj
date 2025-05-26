import './App.css';
import LandingPage from './pages/LandingPage';
import Login from './pages/Login';
import FindAccount from './pages/FindAccount';
import Register from './pages/Register';
import Main from './pages/Main';
import Task from './pages/Task';
import Chat from './pages/Chat';
import {Route, Routes} from 'react-router-dom';

function App() {
  return (
    <div className="App">
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/find-account" element={<FindAccount />} />
        <Route path="/register" element={<Register />} />
        <Route path="/main" element={<Main />} />
        <Route path="/task" element={<Task />} />
        <Route path="/chat" element={<Chat />} />
      </Routes>
    </div>
  );
}

export default App;
