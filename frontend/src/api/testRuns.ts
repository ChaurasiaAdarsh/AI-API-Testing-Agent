import apiClient from './client';
import { TestRun, TestCase, TestExecution, TestRunSummary } from '../types';

export const testRunsApi = {
  getById: (id: number): Promise<TestRun> =>
    apiClient.get(`/test-runs/${id}`).then(r => r.data),

  execute: (id: number): Promise<TestRun> =>
    apiClient.post(`/test-runs/${id}/execute`).then(r => r.data),

  getTestCases: (id: number): Promise<TestCase[]> =>
    apiClient.get(`/test-runs/${id}/test-cases`).then(r => r.data),

  getResults: (id: number): Promise<TestExecution[]> =>
    apiClient.get(`/test-runs/${id}/results`).then(r => r.data),

  getViolations: (id: number): Promise<TestExecution[]> =>
    apiClient.get(`/test-runs/${id}/violations`).then(r => r.data),

  getSummary: (id: number): Promise<TestRunSummary> =>
    apiClient.get(`/test-runs/${id}/summary`).then(r => r.data),
};
