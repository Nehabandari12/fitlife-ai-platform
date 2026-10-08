import axios from "axios"

// The gateway. Who the caller is comes from the bearer token; the gateway ignores any X-User-ID.
export const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'

const api = axios.create({
    baseURL: API_URL
});

api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
});

export const getActivities = () => api.get('/activities');
export const addActivity = (activity) => api.post('/activities', activity);
export const getActivityDetail = (id) => api.get(`/activities/${id}`);
export const getActivityRecommendation = (id) =>
  api.get(`/recommendations/activity/${id}`);
export const getUserRecommendation = (userId) =>
  api.get(`/recommendations/user/${userId}`);

export const deleteActivity = (id) => api.delete(`/activities/${id}`);
export const updateActivity = (id, data) => api.put(`/activities/${id}`, data);

