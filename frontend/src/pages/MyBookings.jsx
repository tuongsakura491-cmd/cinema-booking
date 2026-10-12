import { useEffect, useState } from 'react';
import api, { errMsg, fmt, money } from '../api';
import PayPanel from '../components/PayPanel';

const LABEL = {
    PENDING: 'Chờ thanh toán', CONFIRMED: 'Đã đặt', CANCELLED: 'Đã hủy', EXPIRED: 'Hết hạn', FAILED: 'Thất bại',
};

export default function MyBookings() {
    const [list, setList] = useState([]);
    const [msg, setMsg] = useState('');
    const [payingId, setPayingId] = useState(null);

    const load = () => api.get('/api/bookings/my').then((r) => setList(r.data)).catch((e) => setMsg(errMsg(e)));
    useEffect(() => { load(); }, []);

    const cancel = async (id) => {
        setMsg('');
        try { await api.post(`/api/bookings/${id}/cancel`); } catch (e) { setMsg(errMsg(e)); }
        load();
    };

    const visible = list.filter((b) => b.status !== 'FAILED');

    return (
        <>
            <h2>Vé của tôi</h2>
            {msg && <p className="msg">{msg}</p>}
            {visible.length === 0 && <p className="muted">Bạn chưa có booking nào.</p>}
            {visible.map((b) => (
                <div key={b.id} className="card">
                    <div className="row between">
                        <h3>{b.movieTitle || `Booking #${b.id}`}</h3>
                        <span className={`badge ${b.status}`}>{LABEL[b.status]}</span>
                    </div>
                    <p className="muted">{b.cinemaName} · {b.roomName} · {fmt(b.startTime)}</p>
                    <p>Ghế: <b>{b.seats.map((s) => s.label).join(', ')}</b> · Tổng: <b>{money(b.totalPrice)}</b></p>
                    <div className="row">
                        {b.status === 'PENDING' && (
                            <button className="btn small" onClick={() => setPayingId(payingId === b.id ? null : b.id)}>
                                {payingId === b.id ? 'Đóng' : 'Thanh toán'}
                            </button>
                        )}
                        {(b.status === 'PENDING' || b.status === 'CONFIRMED') && (
                            <button className="btn small ghost" onClick={() => cancel(b.id)}>
                                {b.status === 'CONFIRMED' ? 'Hủy vé (hoàn tiền)' : 'Hủy'}
                            </button>
                        )}
                    </div>
                    {payingId === b.id && (
                        <PayPanel booking={b}
                                  onPaid={() => { setPayingId(null); load(); }}
                                  onExpired={() => { setPayingId(null); setMsg('Booking đã hết hạn giữ ghế.'); load(); }} />
                    )}
                </div>
            ))}
        </>
    );
}
