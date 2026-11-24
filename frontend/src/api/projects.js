import apiClient from './client';

export const projectsApi = {
  getAll: () =>
    apiClient.get('/projects').then(r => r.data),

  getById: (id) =>
    apiClient.get(`/projects/${id}`).then(r => r.data),

  create: (data) =>
    apiClient.post('/projects', data).then(r => r.data),

  uploadSpec: (projectId, content, format) =>
    apiClient.post(`/projects/${projectId}/specifications`, { content, format }).then(r => r.data),

  getSpec: (projectId) =>
    apiClient.get(`/projects/${projectId}/specifications`).then(r => r.data),

  generateTests: (projectId, options) =>
    apiClient.post(`/projects/${projectId}/generate-tests`, options).then(r => r.data),
};
