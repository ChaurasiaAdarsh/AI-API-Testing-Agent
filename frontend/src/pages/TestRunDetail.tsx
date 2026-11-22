import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { testRunsApi } from '../api/testRuns';
import { StatusBadge, MethodBadge } from '../components/StatusBadge';
import { AiLabel } from '../components/AiLabel';
import { JsonViewer } from '../components/JsonViewer';
import {
  ArrowLeft,
  CheckCircle,
  AlertTriangle,
  Clock,
  ChevronDown,
  ChevronRight,
} from 'lucide-react';
import { TestExecution } from '../types';
import { format } from 'date-fns';

function ExecutionRow({ exec }: { exec: TestExecution }) {
  const [expanded, setExpanded] = useState(false);
  const isPassed = exec.status === 'PASSED';

  return (
    <>
      <tr onClick={() => setExpanded(!expanded)} style={{ cursor: 'pointer' }}>
        <td style={{ width: 24 }}>
          {expanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
        </td>
        <td style={{ fontWeight: 600 }}>{exec.testName}</td>
        <td><MethodBadge method={exec.method} /></td>
        <td style={{ fontFamily: 'monospace', fontSize: 12 }}>{exec.endpoint}</td>
        <td style={{ fontFamily: 'monospace' }}>{exec.expectedStatus}</td>
        <td style={{
          fontFamily: 'monospace',
          color: isPassed ? 'var(--accent-green)' : exec.actualStatus ? 'var(--accent-red)' : 'inherit'
        }}>
          {exec.actualStatus ?? '—'}
        </td>
        <td><StatusBadge status={exec.status} /></td>
        <td>
          {exec.contractViolation && (
            <span className="badge badge-violation">
              <AlertTriangle size={10} /> Violation
            </span>
          )}
        </td>
        <td style={{ fontSize: 12, color: 'var(--text-muted)' }}>
          {exec.executionTimeMs ? `${exec.executionTimeMs}ms` : '—'}
        </td>
      </tr>
      {expanded && (
        <tr>
          <td colSpan={9} style={{ background: 'var(--bg-secondary)', padding: 16 }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
              <div>
                <JsonViewer data={exec.requestBody} label="Request Payload" />
                <div style={{ marginTop: 12 }}>
                  <JsonViewer data={exec.actualResponseBody} label="Actual Response Body" />
                </div>
              </div>
              <div>
                {exec.contractViolation && (
                  <div style={{
                    background: 'rgba(210,153,34,0.1)',
                    border: '1px solid rgba(210,153,34,0.3)',
                    borderRadius: 6,
                    padding: 12,
                    marginBottom: 12
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--accent-yellow)', fontWeight: 600, fontSize: 12, marginBottom: 4 }}>
                      <AlertTriangle size={14} /> Contract Violation Detected
                    </div>
                    <div style={{ fontSize: 13 }}>{exec.contractViolationDetails}</div>
                  </div>
                )}

                {exec.failureAnalysis && (
                  <div className="ai-box">
                    <AiLabel text="AI Failure Analysis" />
                    <div style={{ marginTop: 10, display: 'flex', flexDirection: 'column', gap: 8 }}>
                      <div>
                        <div className="label">Root Cause Hypothesis</div>
                        <div style={{ fontSize: 13 }}>{exec.failureAnalysis.likelyCause}</div>
                      </div>
                      <div style={{ display: 'flex', gap: 20 }}>
                        <div>
                          <div className="label">Severity</div>
                          <span className={`severity-${exec.failureAnalysis.severity.toLowerCase()}`}>
                            {exec.failureAnalysis.severity}
                          </span>
                        </div>
                        <div>
                          <div className="label">Expected Negative Test?</div>
                          <span style={{
                            color: exec.failureAnalysis.isExpectedFailure ? 'var(--accent-green)' : 'var(--accent-red)',
                            fontWeight: 600
                          }}>
                            {exec.failureAnalysis.isExpectedFailure ? 'Yes' : 'No'}
                          </span>
                        </div>
                      </div>
                      <div>
                        <div className="label">Recommended Action</div>
                        <div style={{ fontSize: 13, color: 'var(--accent-cyan)' }}>
                          {exec.failureAnalysis.recommendation}
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </td>
        </tr>
      )}
    </>
  );
}

export default function TestRunDetail() {
  const { id } = useParams<{ id: string }>();
  const runId = Number(id);
  const [activeTab, setActiveTab] = useState('summary');

  const { data: testRun, isLoading } = useQuery({
    queryKey: ['testRun', runId],
    queryFn: () => testRunsApi.getById(runId),
    refetchInterval: (query) => {
      const s = query.state.data?.status;
      return s === 'RUNNING' || s === 'GENERATING_TESTS' || s === 'CREATING' ? 2000 : false;
    },
  });

  const { data: results = [] } = useQuery({
    queryKey: ['testResults', runId],
    queryFn: () => testRunsApi.getResults(runId),
    enabled: testRun?.status === 'COMPLETED',
  });

  const { data: violations = [] } = useQuery({
    queryKey: ['violations', runId],
    queryFn: () => testRunsApi.getViolations(runId),
    enabled: testRun?.status === 'COMPLETED',
  });

  const { data: summary } = useQuery({
    queryKey: ['summary', runId],
    queryFn: () => testRunsApi.getSummary(runId),
    enabled: testRun?.status === 'COMPLETED',
  });

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: 60 }}>
        <div className="spinner" style={{ width: 32, height: 32 }} />
      </div>
    );
  }
  if (!testRun) return <div>Test run not found</div>;

  const passRate =
    testRun.totalTests > 0
      ? Math.round((testRun.passedTests / testRun.totalTests) * 100)
      : 0;

  return (
    <div>
      <div style={{ marginBottom: 20 }}>
        <Link
          to={`/projects/${testRun.projectId}`}
          className="btn btn-secondary btn-sm"
          style={{ display: 'inline-flex', marginBottom: 12 }}
        >
          <ArrowLeft size={14} /> Back to Project
        </Link>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h1 style={{ fontSize: 26, fontWeight: 700 }}>Test Run #{testRun.id}</h1>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 4 }}>
              <StatusBadge status={testRun.status} />
              {testRun.startedAt && (
                <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                  <Clock size={12} style={{ display: 'inline', marginRight: 4 }} />
                  {format(new Date(testRun.startedAt), 'MMM d, HH:mm:ss')}
                </span>
              )}
              {testRun.executionTimeMs != null && (
                <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                  {(testRun.executionTimeMs / 1000).toFixed(1)}s elapsed
                </span>
              )}
            </div>
          </div>
        </div>
      </div>

      {testRun.status === 'COMPLETED' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 12, marginBottom: 20 }}>
          <div className="card card-sm">
            <div className="stat-value">{testRun.totalTests}</div>
            <div className="stat-label">Total Executions</div>
          </div>
          <div className="card card-sm">
            <div className="stat-value" style={{ color: 'var(--accent-green)' }}>{testRun.passedTests}</div>
            <div className="stat-label">Passed</div>
          </div>
          <div className="card card-sm">
            <div className="stat-value" style={{ color: 'var(--accent-red)' }}>{testRun.failedTests}</div>
            <div className="stat-label">Failed</div>
          </div>
          <div className="card card-sm">
            <div className="stat-value" style={{ color: 'var(--accent-yellow)' }}>{testRun.contractViolations}</div>
            <div className="stat-label">Contract Violations</div>
          </div>
          <div className="card card-sm">
            <div className="stat-value" style={{ color: passRate >= 80 ? 'var(--accent-green)' : 'var(--accent-yellow)' }}>
              {passRate}%
            </div>
            <div className="stat-label">Pass Rate</div>
          </div>
        </div>
      )}

      {testRun.status === 'COMPLETED' && (
        <>
          <div className="tabs">
            {[
              ['summary', 'Summary'],
              ['results', `Results (${results.length})`],
              ['violations', `Violations (${violations.length})`],
              ['analysis', 'AI Failure Diagnostics'],
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

          {activeTab === 'summary' && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
              <div className="card">
                <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 12 }}>Execution Metrics</h3>
                <div style={{ fontSize: 32, fontWeight: 700, color: passRate >= 80 ? 'var(--accent-green)' : 'var(--accent-yellow)', marginBottom: 8 }}>
                  {passRate}%
                </div>
                <div style={{ display: 'flex', gap: 16, fontSize: 13 }}>
                  <span style={{ color: 'var(--accent-green)' }}>{testRun.passedTests} Passed</span>
                  <span style={{ color: 'var(--accent-red)' }}>{testRun.failedTests} Failed</span>
                  <span style={{ color: 'var(--accent-yellow)' }}>{testRun.contractViolations} Violations</span>
                </div>
              </div>

              {summary?.aiSummary && (
                <div className="card">
                  <AiLabel text="AI Executive Summary" />
                  <p style={{ marginTop: 10, color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                    {summary.aiSummary}
                  </p>
                </div>
              )}
            </div>
          )}

          {activeTab === 'results' && (
            <div className="card" style={{ padding: 0 }}>
              <table>
                <thead>
                  <tr>
                    <th></th>
                    <th>Scenario</th>
                    <th>Method</th>
                    <th>Endpoint</th>
                    <th>Expected</th>
                    <th>Actual</th>
                    <th>Status</th>
                    <th>Violation</th>
                    <th>Time</th>
                  </tr>
                </thead>
                <tbody>
                  {results.map((exec) => (
                    <ExecutionRow key={exec.id} exec={exec} />
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {activeTab === 'violations' && (
            violations.length === 0 ? (
              <div className="empty-state card">
                <CheckCircle size={36} color="var(--accent-green)" />
                <p style={{ color: 'var(--accent-green)', fontWeight: 600, marginTop: 8 }}>
                  Zero contract violations found! API complies strictly with specification.
                </p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                {violations.map((exec) => (
                  <div key={exec.id} className="card" style={{ borderColor: 'rgba(210,153,34,0.3)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                      <AlertTriangle size={16} color="var(--accent-yellow)" />
                      <MethodBadge method={exec.method} />
                      <span style={{ fontFamily: 'monospace' }}>{exec.endpoint}</span>
                    </div>
                    <div style={{ color: 'var(--accent-yellow)', fontSize: 13, background: 'rgba(210,153,34,0.08)', padding: 8, borderRadius: 4 }}>
                      {exec.contractViolationDetails}
                    </div>
                  </div>
                ))}
              </div>
            )
          )}

          {activeTab === 'analysis' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {results.filter(r => r.failureAnalysis).map((exec) => (
                <div key={exec.id} className="card ai-box">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
                    <MethodBadge method={exec.method} />
                    <span style={{ fontFamily: 'monospace', fontWeight: 600 }}>{exec.endpoint}</span>
                    <span className={`severity-${exec.failureAnalysis!.severity.toLowerCase()}`}>
                      [{exec.failureAnalysis!.severity}]
                    </span>
                  </div>
                  <div style={{ fontSize: 13, marginBottom: 6 }}>{exec.failureAnalysis!.summary}</div>
                  <div style={{ fontSize: 12, color: 'var(--accent-cyan)' }}>
                    Action: {exec.failureAnalysis!.recommendation}
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
