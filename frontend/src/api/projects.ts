import apiClient from './client';
import { Project, ApiSpecification, TestRun } from '../types';

export const projectsApi = {
  getAll: (): Promise<Project[]> =>
    apiClient.get('/projects').then(r => r.data),

  getById: (id: number): Promise<Project> =>
    apiClient.get(`/projects/${id}`).then(r => r.data),

  create: (data: { name: string; description?: string; targetBaseUrl: string }): Promise<Project> =>
    apiClient.post('/projects', data).then(r => r.data),

  uploadSpec: (projectId: number, content: string, format: string): Promise<ApiSpecification> =>
    apiClient.post(`/projects/${projectId}/specifications`, { content, format }).then(r => r.data),

  getSpec: (projectId: number): Promise<ApiSpecification> =>
    apiClient.get(`/projects/${projectId}/specifications`).then(r => r.data),

  generateTests: (
    projectId: number,
    options: { maxTests?: number; targetBaseUrl?: string }
  ): Promise<TestRun> =>
    apiClient.post(`/projects/${projectId}/generate-tests`, options).then(r => r.data),
};
