import { useState, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useDropzone } from 'react-dropzone';
import { projectsApi } from '../api/projects';
import { testRunsApi } from '../api/testRuns';
import { StatusBadge, MethodBadge } from '../components/StatusBadge';
import { AiBadge } from '../components/AiLabel';
import {
  ArrowLeft,
  Upload,
  Play,
  Zap,
  FileText,
  CheckCircle,
  ChevronRight,
  AlertCircle,
} from 'lucide-react';
import { TestCase } from '../types';
import { format } from 'date-fns';

export default function ProjectDetail() {
  const { id } = useParams<{ id: string }>();
  const projectId = Number(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState('overview');
  const [specContent, setSpecContent] = useState('');
  const [specFormat, setSpecFormat] = useState<'YAML' | 'JSON'>('YAML');
  const [currentRunId, setCurrentRunId] = useState<number | null>(null);
  const [maxTests, setMaxTests] = useState(8);

  const { data: project, isLoading: projectLoading } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectsApi.getById(projectId),
    enabled: !!projectId,
  });

  const { data: spec, refetch: refetchSpec } = useQuery({
    queryKey: ['spec', projectId],
    queryFn: () => projectsApi.getSpec(projectId),
    enabled: !!projectId,
    retry: false,
  });

  const { data: currentRun } = useQuery({
    queryKey: ['testRun', currentRunId],
    queryFn: () => testRunsApi.getById(currentRunId!),
    enabled: !!currentRunId,
    refetchInterval: (query) => {
      const s = query.state.data?.status;
      return s === 'GENERATING_TESTS' || s === 'RUNNING' || s === 'CREATING' ? 2000 : false;
    },
  });

  const { data: testCases = [] } = useQuery({
    queryKey: ['testCases', currentRunId],
    queryFn: () => testRunsApi.getTestCases(currentRunId!),
    enabled:
      !!currentRunId &&
      (currentRun?.status === 'READY' ||
        currentRun?.status === 'RUNNING' ||
        currentRun?.status === 'COMPLETED'),
  });

  const uploadMutation = useMutation({
    mutationFn: () => projectsApi.uploadSpec(projectId, specContent, specFormat),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['spec', projectId] });
      refetchSpec();
    },
  });

  const generateMutation = useMutation({
    mutationFn: () => projectsApi.generateTests(projectId, { maxTests }),
    onSuccess: (run) => {
      setCurrentRunId(run.id);
      queryClient.invalidateQueries({ queryKey: ['testRun', run.id] });
      setActiveTab('testcases');
    },
  });

  const executeMutation = useMutation({
    mutationFn: () => testRunsApi.execute(currentRunId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['testRun', currentRunId] });
    },
  });

  const onDrop = useCallback((acceptedFiles: File[]) => {
    const file = acceptedFiles[0];
    if (!file) return;
    setSpecFormat(file.name.endsWith('.json') ? 'JSON' : 'YAML');
    const reader = new FileReader();
    reader.onload = (e) => setSpecContent(e.target?.result as string);
    reader.readAsText(file);
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'text/yaml': ['.yaml', '.yml'],
      'application/json': ['.json'],
    },
    maxFiles: 1,
  });

  if (projectLoading) {
    return (
      <div style={{ textAlign: 'center', padding: 60 }}>
        <div className="spinner" style={{ width: 32, height: 32 }} />
      </div>
    );
  }
  if (!project) return <div>Project not found</div>;

  const isActive =
    currentRun?.status === 'GENERATING_TESTS' ||
    currentRun?.status === 'RUNNING' ||
    currentRun?.status === 'CREATING';
  const isReady = currentRun?.status === 'READY';
  const isCompleted = currentRun?.status === 'COMPLETED';

  return (
    <div>
      <div style={{ marginBottom: 20 }}>
        <Link
          to="/projects"
          className="btn btn-secondary btn-sm"
          style={{ display: 'inline-flex', marginBottom: 12 }}
        >
          <ArrowLeft size={14} /> Projects
        </Link>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h1 style={{ fontSize: 26, fontWeight: 700 }}>{project.name}</h1>
            <div style={{ fontFamily: 'monospace', fontSize: 13, color: 'var(--accent-cyan)' }}>
              {project.targetBaseUrl}
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {spec && !isActive && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>Tests:</span>
                <select
                  className="input input-sm"
                  style={{ width: 85, padding: '4px 8px', cursor: 'pointer' }}
                  value={maxTests}
                  onChange={(e) => setMaxTests(Number(e.target.value))}
                  disabled={isActive || generateMutation.isPending}
                >
                  <option value={4}>4 tests</option>
                  <option value={6}>6 tests</option>
                  <option value={8}>8 (Fast)</option>
                  <option value={12}>12 tests</option>
                  <option value={20}>20 tests</option>
                </select>
              </div>
            )}
            {spec && !isActive && (
              <button
                className="btn btn-ai"
                onClick={() => generateMutation.mutate()}
                disabled={generateMutation.isPending || isActive}
              >
                {generateMutation.isPending ? (
                  <>
                    <div className="spinner" /> Generating...
                  </>
                ) : (
                  <>
                    <Zap size={15} /> Generate AI Tests
                  </>
                )}
              </button>
            )}
            {isReady && (
              <button
                className="btn btn-primary"
                onClick={() => executeMutation.mutate()}
                disabled={executeMutation.isPending}
              >
                {executeMutation.isPending ? (
                  <>
                    <div className="spinner" /> Running...
                  </>
                ) : (
                  <>
                    <Play size={15} /> Execute Tests
                  </>
                )}
              </button>
            )}
          </div>
        </div>
      </div>

      {isActive && (
        <div
          className="card"
          style={{
            marginBottom: 16,
            borderLeft: '4px solid var(--accent-cyan)',
            background: 'rgba(0, 212, 255, 0.05)',
            display: 'flex',
            alignItems: 'center',
            gap: 12,
          }}
        >
          <div className="spinner" style={{ width: 18, height: 18 }} />
          <div>
            <div style={{ fontWeight: 600, color: 'var(--accent-cyan)' }}>
              {currentRun?.status === 'GENERATING_TESTS'
                ? 'AI is generating test scenarios via Ollama (llama3.2)...'
                : 'Executing tests against target API...'}
            </div>
            <div style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 2 }}>
              {currentRun?.status === 'GENERATING_TESTS'
                ? `Analyzing OpenAPI contract schemas and constraints to produce positive, negative & boundary test cases.`
                : 'Running HTTP requests and validating response status, schemas & headers.'}
            </div>
          </div>
        </div>
      )}

      {currentRun?.status === 'FAILED' && (
        <div
          className="card"
          style={{
            marginBottom: 16,
            borderLeft: '4px solid var(--accent-red)',
            background: 'rgba(255, 77, 77, 0.05)',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <div style={{ fontWeight: 600, color: 'var(--accent-red)', display: 'flex', alignItems: 'center', gap: 8 }}>
                <AlertCircle size={16} /> Test Generation Failed
              </div>
              <div style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 4 }}>
                {currentRun.errorMessage || 'An error occurred during AI test generation.'}
              </div>
            </div>
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => generateMutation.mutate()}
              disabled={generateMutation.isPending}
            >
              Retry
            </button>
          </div>
        </div>
      )}

      {currentRun && (
        <div
          className="card card-sm"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: 20,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            {isActive && <div className="spinner" />}
            {isCompleted && <CheckCircle size={16} color="var(--accent-green)" />}
            <div>
              <span style={{ fontWeight: 600 }}>Test Run #{currentRun.id}</span>
              <span style={{ marginLeft: 8 }}><StatusBadge status={currentRun.status} /></span>
              {isCompleted && (
                <span style={{ marginLeft: 12, fontSize: 13 }}>
                  <span style={{ color: 'var(--accent-green)' }}>{currentRun.passedTests} passed</span>
                  {' · '}
                  <span style={{ color: 'var(--accent-red)' }}>{currentRun.failedTests} failed</span>
                  {currentRun.contractViolations > 0 && (
                    <span style={{ color: 'var(--accent-yellow)', marginLeft: 6 }}>
                      ({currentRun.contractViolations} violations)
                    </span>
                  )}
                </span>
              )}
            </div>
          </div>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => navigate(`/test-runs/${currentRun.id}`)}
          >
            Report Details <ChevronRight size={14} />
          </button>
        </div>
      )}

      <div className="tabs">
        {[
          ['overview', 'Overview'],
          ['specification', 'OpenAPI Specification'],
          ['testcases', `Generated Tests (${testCases.length})`],
        ].map(([key, label]) => (
          <button
            key={key}
            className={`tab ${activeTab === key ? 'active' : ''}`}
            onClick={() => setActiveTab(key)}
          >
            {label}
          </button>
        ))}
      </div>

      {activeTab === 'overview' && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
          <div className="card">
            <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 12 }}>Target Connection</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div>
                <div className="label">Target Base URL</div>
                <div style={{ fontFamily: 'monospace', color: 'var(--accent-cyan)' }}>{project.targetBaseUrl}</div>
              </div>
              <div>
                <div className="label">API Contract Spec</div>
                {spec ? (
                  <span style={{ color: 'var(--accent-green)' }}>
                    ✓ {spec.title} v{spec.version} ({spec.format})
                  </span>
                ) : (
                  <span style={{ color: 'var(--accent-red)' }}>✗ Specification not uploaded</span>
                )}
              </div>
            </div>
          </div>

          <div className="card">
            <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 12 }}>Test Configuration</h3>
            <div className="form-group">
              <label className="label">Max Tests to Generate</label>
              <input
                type="number"
                className="input"
                value={maxTests}
                min={5}
                max={100}
                onChange={(e) => setMaxTests(Number(e.target.value))}
              />
            </div>
            {spec && !isActive && (
              <button
                className="btn btn-ai"
                style={{ width: '100%' }}
                onClick={() => generateMutation.mutate()}
                disabled={generateMutation.isPending}
              >
                <Zap size={15} /> Trigger AI Test Generation
              </button>
            )}
          </div>
        </div>
      )}

      {activeTab === 'specification' && (
        <div style={{ maxWidth: 800 }}>
          {spec && (
            <div className="card card-sm" style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
              <FileText size={18} color="var(--accent-blue)" />
              <div style={{ flex: 1 }}>
                <span style={{ fontWeight: 600 }}>{spec.title}</span> (v{spec.version})
                <span style={{ fontSize: 12, color: 'var(--text-muted)', marginLeft: 8 }}>
                  Uploaded {format(new Date(spec.uploadedAt), 'MMM d, yyyy HH:mm')}
                </span>
              </div>
              <span className="badge badge-pass">Active</span>
            </div>
          )}

          <div className="card">
            <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>
              {spec ? 'Replace Specification' : 'Upload OpenAPI Specification'}
            </h3>

            <div className="form-group">
              <label className="label">Format</label>
              <div style={{ display: 'flex', gap: 8 }}>
                {(['YAML', 'JSON'] as const).map((fmt) => (
                  <button
                    key={fmt}
                    className={`btn ${specFormat === fmt ? 'btn-primary' : 'btn-secondary'} btn-sm`}
                    onClick={() => setSpecFormat(fmt)}
                  >
                    {fmt}
                  </button>
                ))}
              </div>
            </div>

            <div
              {...getRootProps()}
              style={{
                border: `2px dashed ${isDragActive ? 'var(--accent-blue)' : 'var(--border-color)'}`,
                borderRadius: 6,
                padding: 24,
                textAlign: 'center',
                cursor: 'pointer',
                background: isDragActive ? 'rgba(88,166,255,0.06)' : 'var(--bg-secondary)',
                marginBottom: 16,
              }}
            >
              <input {...getInputProps()} />
              <Upload size={24} color="var(--text-muted)" style={{ marginBottom: 8 }} />
              <div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
                {isDragActive ? 'Drop spec file here' : 'Drag & drop OpenAPI file (.yaml/.json), or click to browse'}
              </div>
            </div>

            <div className="form-group">
              <label className="label">Or Paste Specification Content</label>
              <textarea
                className="textarea"
                rows={12}
                value={specContent}
                onChange={(e) => setSpecContent(e.target.value)}
                placeholder={specFormat === 'YAML' ? 'openapi: "3.0.3"\ninfo:\n  title: My API...' : '{\n  "openapi": "3.0.3"...\n}'}
              />
            </div>

            {uploadMutation.isSuccess && (
              <div style={{ color: 'var(--accent-green)', fontSize: 13, marginBottom: 12 }}>
                ✓ Specification uploaded successfully!
              </div>
            )}
            {uploadMutation.isError && (
              <div className="error-msg" style={{ marginBottom: 12 }}>
                {(uploadMutation.error as Error).message}
              </div>
            )}

            <button
              className="btn btn-primary"
              style={{ width: '100%' }}
              disabled={!specContent.trim() || uploadMutation.isPending}
              onClick={() => uploadMutation.mutate()}
            >
              {uploadMutation.isPending ? 'Uploading...' : 'Save & Parse Contract'}
            </button>
          </div>
        </div>
      )}

      {activeTab === 'testcases' && (
        <div>
          {!currentRun ? (
            <div className="empty-state card">
              <p>No tests generated yet. Go to Overview and click "Generate AI Tests".</p>
            </div>
          ) : isActive ? (
            <div className="card" style={{ textAlign: 'center', padding: 50 }}>
              <div className="spinner" style={{ width: 32, height: 32, marginBottom: 16 }} />
              <div style={{ fontWeight: 600 }}>
                {currentRun.status === 'GENERATING_TESTS' ? 'AI is inspecting OpenAPI schemas...' : 'Executing tests...'}
              </div>
            </div>
          ) : testCases.length === 0 ? (
            <div className="empty-state card">No test cases generated.</div>
          ) : (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ fontWeight: 600 }}>{testCases.length} Scenarios</span>
                  <AiBadge />
                </div>
                {isReady && (
                  <button
                    className="btn btn-primary"
                    onClick={() => executeMutation.mutate()}
                    disabled={executeMutation.isPending}
                  >
                    <Play size={14} /> Execute All Scenarios
                  </button>
                )}
              </div>
              <div className="card" style={{ padding: 0 }}>
                <table>
                  <thead>
                    <tr>
                      <th>Scenario</th>
                      <th>Method</th>
                      <th>Path</th>
                      <th>Category</th>
                      <th>Expected</th>
                      <th>Intent</th>
                    </tr>
                  </thead>
                  <tbody>
                    {testCases.map((tc: TestCase) => (
                      <tr key={tc.id}>
                        <td style={{ fontWeight: 600, fontSize: 13 }}>{tc.name}</td>
                        <td><MethodBadge method={tc.method} /></td>
                        <td style={{ fontFamily: 'monospace', fontSize: 12 }}>{tc.path}</td>
                        <td><span className={`badge badge-${tc.category.toLowerCase()}`}>{tc.category}</span></td>
                        <td><span style={{ fontFamily: 'monospace' }}>{tc.expectedStatus}</span></td>
                        <td style={{ color: 'var(--text-secondary)', fontSize: 12 }}>{tc.purpose}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
