import { Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { getUser } from './api';
import Login from './pages/Login';
import Movies from './pages/Movies';
import MovieDetail from './pages/MovieDetail';
import SeatPicker from './pages/SeatPicker';
import MyBookings from './pages/MyBookings';
import Admin from './pages/Admin';
import NotificationBell from './components/NotificationBell';
function Navbar() {
  useLocation(); // render lại khi chuyển trang để cập nhật trạng thái đăng nhập
  const nav = useNavigate();
  const user = getUser();
  const logout = () => { localStorage.clear(); nav('/login'); };
  return (
      <header className="nav">
        <Link to="/" className="brand">🎬 Cinema</Link>
        <nav>
          {user ? (
              <>
                  {user?.role === 'ADMIN' && <Link to="/admin">Quản trị</Link>}
                <NotificationBell />
                <Link to="/bookings">Vé của tôi</Link>
                <span className="muted">{user.username} ({user.role})</span>
                <button className="btn small" onClick={logout}>Đăng xuất</button>
              </>
          ) : (
              <Link to="/login">Đăng nhập</Link>
          )}
        </nav>
      </header>
  );
}
function RequireAdmin({ children }) {
    const u = getUser();
    return localStorage.getItem('token') && u?.role === 'ADMIN' ? children : <Navigate to="/" replace />;
}
function RequireAuth({ children }) {
  return localStorage.getItem('token') ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
      <>
        <Navbar />
        <main className="container">
          <Routes>
              <Route path="/admin" element={<RequireAdmin><Admin /></RequireAdmin>} />
            <Route path="/" element={<Movies />} />
            <Route path="/login" element={<Login />} />
            <Route path="/movies/:id" element={<MovieDetail />} />
            <Route path="/showtimes/:id" element={<RequireAuth><SeatPicker /></RequireAuth>} />
            <Route path="/bookings" element={<RequireAuth><MyBookings /></RequireAuth>} />
          </Routes>
        </main>
      </>
  );
}
