import React, { useState, useEffect } from 'react';
// import {Settings} from "lucide-react"; // 사용되지 않아 제거

const Setting = () => {
    const [currentUser, setCurrentUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmNewPassword, setConfirmNewPassword] = useState('');
    const [passwordChangeMessage, setPasswordChangeMessage] = useState('');
    const [passwordChangeError, setPasswordChangeError] = useState('');

    useEffect(() => {
        const fetchCurrentUser = async () => {
            try {
                // 현재 사용자 정보를 가져오는 API 호출 (UserController의 /api/users/me 엔드포인트와 연결)
                const response = await fetch('/api/users/me');
                if (response.ok) {
                    const userData = await response.json();
                    setCurrentUser(userData);
                } else {
                    const errorText = await response.text();
                    setError(`사용자 정보 로딩 실패 (${response.status}): ${errorText}`);
                }
            } catch (err) {
                setError('네트워크 오류로 사용자 정보를 가져올 수 없습니다.');
                console.error("사용자 정보 로딩 중 네트워크 오류:", err);
            } finally {
                setLoading(false);
            }
        };
        fetchCurrentUser();
    }, []);

    const handleChangePassword = async (e) => {
        e.preventDefault();
        setPasswordChangeMessage('');
        setPasswordChangeError('');

        if (newPassword !== confirmNewPassword) {
            setPasswordChangeError('새 비밀번호와 비밀번호 확인이 일치하지 않습니다.');
            return;
        }

        if (newPassword.length < 8) {
            setPasswordChangeError('새 비밀번호는 최소 8자 이상이어야 합니다.');
            return;
        }

        try {
            // 비밀번호 변경 API 호출 (UserController의 /api/users/change-password 엔드포인트와 연결)
            const response = await fetch('/api/users/change-password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ currentPassword, newPassword }),
            });

            if (response.ok) {
                setPasswordChangeMessage('비밀번호가 성공적으로 변경되었습니다!');
                setCurrentPassword('');
                setNewPassword('');
                setConfirmNewPassword('');
            } else {
                const errorData = await response.json(); // 백엔드에서 JSON 형태의 에러 메시지를 보낸다고 가정
                setPasswordChangeError(errorData.message || '비밀번호 변경에 실패했습니다.');
            }
        } catch (err) {
            setPasswordChangeError('네트워크 오류로 비밀번호를 변경할 수 없습니다.');
            console.error("비밀번호 변경 중 네트워크 오류:", err);
        }
    };

    if (loading) {
        return (
            <div className="flex justify-center items-center min-h-screen bg-gray-100">
                <p className="text-lg text-gray-700">사용자 정보 로딩 중...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="flex justify-center items-center min-h-screen bg-gray-100">
                <p className="text-lg text-red-500">오류: {error}</p>
            </div>
        );
    }

    if (!currentUser) {
        return (
            <div className="flex justify-center items-center min-h-screen bg-gray-100">
                <p className="text-lg text-gray-700">사용자 정보를 찾을 수 없습니다.</p>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-100 p-8 flex justify-center items-start">
            <div className="bg-white p-8 rounded-lg shadow-xl w-full max-w-2xl">
                <h1 className="text-4xl font-extrabold text-gray-900 mb-8 text-center">설정</h1>

                {/* 사용자 정보 섹션 */}
                <section className="mb-10 p-6 bg-blue-50 rounded-lg border border-blue-200">
                    <h2 className="text-2xl font-bold text-blue-800 mb-4 flex items-center">
                        <svg className="w-7 h-7 mr-2 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                            <path fillRule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clipRule="evenodd"></path>
                        </svg>
                        내 정보
                    </h2>
                    <div className="space-y-3 text-lg">
                        <p className="flex items-center text-gray-700">
                            <strong className="w-24 text-gray-800">사용자 ID:</strong>
                            <span className="bg-gray-100 px-3 py-1 rounded-md break-all">{currentUser.id}</span>
                        </p>
                        <p className="flex items-center text-gray-700">
                            <strong className="w-24 text-gray-800">사용자 이름:</strong>
                            <span className="bg-gray-100 px-3 py-1 rounded-md">{currentUser.username}</span>
                        </p>
                        <p className="flex items-center text-gray-700">
                            <strong className="w-24 text-gray-800">이메일:</strong>
                            <span className="bg-gray-100 px-3 py-1 rounded-md break-all">{currentUser.email}</span>
                        </p>
                        {/* 권한 정보는 currentUser 객체에 'roles' 필드가 있다고 가정 */}
                        {currentUser.roles && (
                            <p className="flex items-center text-gray-700">
                                <strong className="w-24 text-gray-800">권한:</strong>
                                <span className="bg-gray-100 px-3 py-1 rounded-md">
                                    {/* 배열인 경우 쉼표로 구분하여 표시 */}
                                    {Array.isArray(currentUser.roles) ? currentUser.roles.join(', ') : currentUser.roles}
                                </span>
                            </p>
                        )}
                    </div>
                </section>

                {/* 비밀번호 변경 섹션 */}
                <section className="p-6 bg-purple-50 rounded-lg border border-purple-200">
                    <h2 className="text-2xl font-bold text-purple-800 mb-4 flex items-center">
                        <svg className="w-7 h-7 mr-2 text-purple-600" fill="currentColor" viewBox="0 0 20 20">
                            <path fillRule="evenodd" d="M18 8V5a2 2 0 00-2-2H4a2 2 0 00-2 2v3m16 0H2m16 0v5a2 2 0 01-2 2H4a2 2 0 01-2-2v-5m12 9v-3m-3 3h6m-3 3V10" clipRule="evenodd"></path>
                        </svg>
                        비밀번호 변경
                    </h2>
                    <form onSubmit={handleChangePassword} className="space-y-6">
                        <div>
                            <label className="block text-lg font-medium text-gray-700 mb-2" htmlFor="current-password">현재 비밀번호</label>
                            <input
                                type="password"
                                id="current-password"
                                className="mt-1 block w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-purple-500 focus:border-purple-500 sm:text-lg"
                                value={currentPassword}
                                onChange={(e) => setCurrentPassword(e.target.value)}
                                required
                            />
                        </div>
                        <div>
                            <label className="block text-lg font-medium text-gray-700 mb-2" htmlFor="new-password">새 비밀번호</label>
                            <input
                                type="password"
                                id="new-password"
                                className="mt-1 block w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-purple-500 focus:border-purple-500 sm:text-lg"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                required
                                minLength="8"
                            />
                        </div>
                        <div>
                            <label className="block text-lg font-medium text-gray-700 mb-2" htmlFor="confirm-new-password">새 비밀번호 확인</label>
                            <input
                                type="password"
                                id="confirm-new-password"
                                className="mt-1 block w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-purple-500 focus:border-purple-500 sm:text-lg"
                                value={confirmNewPassword}
                                onChange={(e) => setConfirmNewPassword(e.target.value)}
                                required
                                minLength="8"
                            />
                        </div>

                        {passwordChangeError && (
                            <p className="text-red-500 text-sm mt-2">{passwordChangeError}</p>
                        )}
                        {passwordChangeMessage && (
                            <p className="text-green-600 text-sm mt-2">{passwordChangeMessage}</p>
                        )}

                        <button
                            type="submit"
                            className="w-full flex justify-center py-3 px-4 border border-transparent rounded-md shadow-sm text-lg font-medium text-white bg-purple-600 hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500 transition duration-150 ease-in-out"
                        >
                            비밀번호 변경
                        </button>
                    </form>
                </section>
            </div>
        </div>
    );
};

export default Setting;
