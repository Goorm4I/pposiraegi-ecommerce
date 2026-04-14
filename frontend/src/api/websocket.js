import { WS_URL, USE_MOCK } from './config';

class EventWebSocket {
  constructor() {
    this.ws = null;
    this.listeners = [];
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
  }

  connect() {
    // Mock 모드면 연결 안함 (BE 연동 시에만 동작)
    if (USE_MOCK) {
      console.log('Mock 모드: WebSocket 연결 생략');
      return;
    }

    try {
      this.ws = new WebSocket(WS_URL);

      this.ws.onopen = () => {
        console.log('WebSocket connected');
        this.reconnectAttempts = 0;
      };

      this.ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        this.notifyListeners(data);
      };

      this.ws.onclose = () => {
        console.log('WebSocket disconnected');
        this.tryReconnect();
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error);
      };
    } catch (error) {
      console.error('WebSocket connection failed:', error);
    }
  }

  tryReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      setTimeout(() => this.connect(), 3000);
    }
  }

  subscribe(listener) {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter(l => l !== listener);
    };
  }

  notifyListeners(event) {
    this.listeners.forEach(listener => listener(event));
  }

  disconnect() {
    if (this.ws) {
      this.ws.close();
    }
  }
}

export const eventWebSocket = new EventWebSocket();
export default eventWebSocket;
