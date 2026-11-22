import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import ProjectList from './pages/ProjectList';
import ProjectDetail from './pages/ProjectDetail';
import NewProject from './pages/NewProject';
import TestRunDetail from './pages/TestRunDetail';

export default function App() {
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/projects" element={<ProjectList />} />
          <Route path="/projects/new" element={<NewProject />} />
          <Route path="/projects/:id" element={<ProjectDetail />} />
          <Route path="/test-runs/:id" element={<TestRunDetail />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  );
}
