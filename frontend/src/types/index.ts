export interface Project {
  id: number;
  name: string;
  description?: string;
  targetBaseUrl: string;
  specificationCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ApiSpecification {
  id: number;
  title: string;
  version: string;
  format: string;
  uploadedAt: string;
  projectId: number;
}

export type TestRunStatus =
  | 'CREATED'
  | 'CREATING'
  | 'GENERATING_TESTS'
  | 'READY'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED';

export interface TestRun {
  id: number;
  status: TestRunStatus;
  totalTests: number;
  passedTests: number;
  failedTests: number;
  skippedTests: number;
  contractViolations: number;
  executionTimeMs?: number;
  errorMessage?: string;
  startedAt?: string;
  completedAt?: string;
  createdAt: string;
  projectId: number;
  specificationId?: number;
}

export type TestCategory = 'POSITIVE' | 'NEGATIVE' | 'BOUNDARY';
export type ExecutionStatus = 'PENDING' | 'RUNNING' | 'PASSED' | 'FAILED' | 'ERROR' | 'SKIPPED';
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface TestCase {
  id: number;
  name: string;
  method: string;
  path: string;
  category: TestCategory;
  requestBody?: string;
  requestHeaders?: string;
  queryParams?: string;
  expectedStatus: number;
  purpose: string;
  description?: string;
  testRunId: number;
}

export interface FailureAnalysis {
  id: number;
  summary: string;
  likelyCause: string;
  severity: Severity;
  contractViolation: boolean;
  isExpectedFailure: boolean;
  recommendation: string;
  analyzedAt: string;
}

export interface TestExecution {
  id: number;
  testName: string;
  method: string;
  endpoint: string;
  requestBody?: string;
  requestHeaders?: string;
  expectedStatus: number;
  actualStatus?: number;
  actualResponseBody?: string;
  actualResponseHeaders?: string;
  executionTimeMs?: number;
  status: ExecutionStatus;
  contractViolation: boolean;
  contractViolationDetails?: string;
  executedAt?: string;
  testRunId: number;
  failureAnalysis?: FailureAnalysis;
}

export interface TestRunSummary {
  testRun: TestRun;
  passRate: number;
  contractViolationRate: number;
  aiSummary?: string;
  topFailures: string[];
  analysisReady: boolean;
}
