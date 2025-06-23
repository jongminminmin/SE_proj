import { Client } from '@stomp/stompjs';

// 앱 전체에서 단 하나만 존재할 클라이언트 인스턴스
let stompClient = null;
let subscriptions = {}; // 구독 정보를 저장할 객체

// 연결 상태 변경 시 호출될 콜백 함수들을 저장할 배열
const connectListeners = new Set();
const disconnectListeners = new Set();

const createClient = () => {
    return new Client({
        brokerURL: `ws://${window.location.hostname}:9000/ws`,
        reconnectDelay: 5000,
        debug: (str) => {
            console.log(new Date(), '[STOMP Service]', str);
        },
        onConnect: () => {
            console.log("[STOMP Service] 웹소켓 연결 성공.");
            // 모든 연결 리스너들에게 알림
            connectListeners.forEach(listener => listener());
        },
        onDisconnect: () => {
            console.warn("[STOMP Service] 웹소켓 연결 해제됨.");
            // 모든 연결 해제 리스너들에게 알림
            disconnectListeners.forEach(listener => listener());
        },
    });
};

// --- 외부에서 호출할 함수들 ---

// 클라이언트 인스턴스를 가져오는 함수 (없으면 생성)
export const getStompClient = () => {
    if (!stompClient) {
        stompClient = createClient();
    }
    return stompClient;
};

// 앱 시작 시 (로그인 성공 시) 호출할 연결 함수
export const connect = () => {
    const client = getStompClient();
    if (!client.active) {
        console.log("[STOMP Service] 클라이언트 활성화를 시도합니다.");
        client.activate();
    }
};

// 앱 종료 시 (로그아웃 시) 호출할 연결 해제 함수
export const disconnect = () => {
    const client = getStompClient();
    if (client.active) {
        console.log("[STOMP Service] 클라이언트 비활성화를 시도합니다.");
        client.deactivate();
        // 연결 해제 시 모든 구독 정보 초기화
        subscriptions = {};
    }
};

// 특정 채널을 구독하는 함수
export const subscribe = (destination, callback) => {
    const client = getStompClient();
    if (!client.active) {
        console.warn(`[STOMP Service] 클라이언트가 활성화되지 않아 ${destination} 구독을 할 수 없습니다.`);
        return null;
    }
    if (subscriptions[destination]) {
        console.log(`[STOMP Service] 이미 구독 중인 채널입니다: ${destination}`);
        return;
    }

    console.log(`[STOMP Service] 구독 시작: ${destination}`);
    const sub = client.subscribe(destination, callback);
    subscriptions[destination] = sub;
    return sub;
};

// 특정 채널의 구독을 취소하는 함수
export const unsubscribe = (destination) => {
    if (subscriptions[destination]) {
        console.log(`[STOMP Service] 구독 취소: ${destination}`);
        subscriptions[destination].unsubscribe();
        delete subscriptions[destination];
    }
};

// 메시지를 발행하는 함수
export const publish = (options) => {
    const client = getStompClient();
    if (client.active) {
        client.publish(options);
    } else {
        console.error("[STOMP Service] 클라이언트가 활성화되지 않아 메시지를 발행할 수 없습니다.");
    }
};

// 연결 상태 변화를 감지할 리스너를 등록하고, 해제 함수를 반환
export const addConnectListener = (callback) => {
    connectListeners.add(callback);
    return () => connectListeners.delete(callback);
};

export const addDisconnectListener = (callback) => {
    disconnectListeners.add(callback);
    return () => disconnectListeners.delete(callback);
};
