// API 설정
export const USE_MOCK = process.env.REACT_APP_USE_MOCK === 'true';

export const API_BASE_URL = process.env.REACT_APP_API_URL || '';
export const WS_URL = process.env.REACT_APP_WS_URL || `ws://${window.location.host}/ws/events`;

export default { USE_MOCK, API_BASE_URL, WS_URL };
