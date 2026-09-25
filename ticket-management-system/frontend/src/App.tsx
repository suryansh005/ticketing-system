import { Navigate, Route, Routes } from 'react-router-dom';
import { TicketList } from './components/TicketList';
import { NewTicketPage } from './pages/NewTicketPage';
import { TicketDetailPage } from './pages/TicketDetailPage';

export default function App() {
  return (
    <div className="app-shell">
      <Routes>
        <Route path="/" element={<Navigate to="/tickets" replace />} />
        <Route path="/tickets" element={<TicketList />} />
        <Route path="/tickets/new" element={<NewTicketPage />} />
        <Route path="/tickets/:ticketId" element={<TicketDetailPage />} />
      </Routes>
    </div>
  );
}
