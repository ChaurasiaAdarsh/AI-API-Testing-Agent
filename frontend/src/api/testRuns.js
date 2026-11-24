import apiClient from './client';

export const testRunsApi = {
  getById: (id) =>
    apiClient.get(`/test-runs/${id}`).then(r => r.data),

  execute: (id) =>
    apiClient.post(`/test-runs/${id}/execute`).then(r => r.data),

  getTestCases: (id) =>
    apiClient.get(`/test-runs/${id}/test-cases`).then(r => r.data),

  getResults: (id) =>
    apiClient.get(`/test-runs/${id}/results`).then(r => r.data),

  getViolations: (id) =>
    apiClient.get(`/test-runs/${id}/violations`).then(r => r.data),

  getSummary: (id) =>
    apiClient.get(`/test-runs/${id}/summary`).then(r => r.data),
};
