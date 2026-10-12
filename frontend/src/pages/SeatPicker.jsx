import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api, { errMsg, fmt, money } from '../api';
import PayPanel from '../components/PayPanel';

export default function SeatPicker() {
    const { id } = useParams();
    const nav = useNavigate();
    const [st, setSt] = useState(null);
    const [seats, setSeats] = useState([]);
    const [sel, setSel] = useState([]);
    const [booking, setBooking] = useState(null);
    const [left, setLeft] = useState(0);
    const [msg, setMsg] = useState('');
    const [busy, setBusy] = useState(false);
    const deadline = useRef(0);

    const loadSeats = useCallback(
        () => api.get(`/api/showtimes/${id}/seats`).then((r) => {
            setSeats(r.data);
            // bỏ khỏi danh sách chọn những ghế không còn trống
            setSel((s) => s.filter((x) => r.data.some((y) => y.seatId === x && y.status === 'AVAILABLE')));
        }),
        [id]
    );

    useEffect(() => {
        api.get(`/api/showtimes/${id}`).then((r) => setSt(r.data)).catch((e) => setMsg(errMsg(e)));
        loadSeats();
    }, [id, loadSeats]);

    // Polling sơ đồ ghế 8 giây một lần khi chưa giữ ghế
    useEffect(() => {
        if (booking) return;
        const t = setInterval(loadSeats, 8000);
        return () => clearInterval(t);
    }, [booking, loadSeats]);

    // Đồng hồ đếm ngược
    useEffect(() => {
        if (!booking) return;
        const t = setInterval(() => {
            const s = Math.max(0, Math.round((deadline.current - Date.now()) / 1000));
            setLeft(s);
            if (s === 0) {
                clearInterval(t);
                setMsg('Đã hết thời gian giữ ghế, vui lòng chọn lại.');
                setBooking(null);
                setSel([]);
                loadSeats();
            }
        }, 500);
        return () => clearInterval(t);
    }, [booking, loadSeats]);

    const toggle = (s) => {
        if (booking || s.status !== 'AVAILABLE') return;
        setSel((cur) => (cur.includes(s.seatId) ? cur.filter((x) => x !== s.seatId) : cur.length < 8 ? [...cur, s.seatId] : cur));
    };

    const hold = async () => {
        setBusy(true);
        setMsg('');
        try {
            const { data } = await api.post('/api/bookings', { showtimeId: Number(id), seatIds: sel });
            // Tính thời gian còn lại bằng (hết hạn - tạo) để không phụ thuộc múi giờ giữa máy chủ và trình duyệt
            const ms = new Date(data.holdExpiresAt) - new Date(data.createdAt);
            deadline.current = Date.now() + ms;
            setLeft(Math.round(ms / 1000));
            setBooking(data);
            loadSeats();
        } catch (e) {
            setMsg(e.response?.status === 409 ? 'Có ghế vừa bị người khác giữ. Sơ đồ ghế đã được tải lại, hãy chọn lại.' : errMsg(e));
            setSel([]);
            loadSeats();
        } finally {
            setBusy(false);
        }
    };

    // Thanh toán (PayPanel) báo lại khi hết hạn giữ ghế
    const onExpired = () => {
        setMsg('Đã hết hạn giữ ghế, vui lòng chọn lại. Nếu đã bị trừ tiền, hệ thống sẽ hoàn lại.');
        setBooking(null);
        setSel([]);
        loadSeats();
    };

    const cancel = async () => {
        try { await api.post(`/api/bookings/${booking.id}/cancel`); } catch (e) { /* bỏ qua */ }
        setBooking(null);
        setSel([]);
        loadSeats();
    };

    const rows = seats.reduce((acc, s) => ((acc[s.rowLabel] = acc[s.rowLabel] || []).push(s), acc), {});
    const total = seats.filter((s) => sel.includes(s.seatId)).reduce((sum, s) => sum + s.price, 0);
    const mm = String(Math.floor(left / 60)).padStart(2, '0');
    const ss = String(left % 60).padStart(2, '0');

    return (
        <>
            {st && (
                <div className="card">
                    <h2>{st.movieTitle}</h2>
                    <p className="muted">{st.cinemaName} · {st.roomName} · {fmt(st.startTime)}</p>
                </div>
            )}

            <div className="screen">MÀN HÌNH</div>
            <div className="seatmap">
                {Object.entries(rows).map(([row, list]) => (
                    <div key={row} className="seatrow">
                        <span className="rowlabel">{row}</span>
                        {list.map((s) => {
                            const mine = booking?.seats.some((x) => x.seatId === s.seatId);
                            const cls = mine ? 'mine' : sel.includes(s.seatId) ? 'selected' : s.status !== 'AVAILABLE' ? 'taken' : s.type === 'VIP' ? 'vip' : '';
                            return (
                                <button key={s.seatId} className={`seat ${cls}`} onClick={() => toggle(s)}
                                        title={`${s.label} · ${money(s.price)}`} disabled={s.status !== 'AVAILABLE' && !mine}>
                                    {s.seatNumber}
                                </button>
                            );
                        })}
                    </div>
                ))}
            </div>

            <div className="legend">
                <span><i className="seat" /> Thường</span>
                <span><i className="seat vip" /> VIP</span>
                <span><i className="seat selected" /> Đang chọn</span>
                <span><i className="seat taken" /> Đã giữ/bán</span>
            </div>

            {msg && <p className="msg">{msg}</p>}

            {!booking ? (
                <div className="card row between">
                    <div>
                        <b>Ghế đã chọn:</b> {seats.filter((s) => sel.includes(s.seatId)).map((s) => s.label).join(', ') || 'chưa chọn'}
                        <div>Tạm tính: <b>{money(total)}</b></div>
                    </div>
                    <button className="btn" disabled={sel.length === 0 || busy} onClick={hold}>Giữ ghế &amp; đặt vé</button>
                </div>
            ) : (
                <div className="card">
                    <h3>Đã giữ ghế: {booking.seats.map((s) => s.label).join(', ')}</h3>
                    <p>Tổng tiền: <b>{money(booking.totalPrice)}</b></p>
                    <p className="timer">Thời gian giữ ghế còn lại: {mm}:{ss}</p>
                    <PayPanel booking={booking} onPaid={() => nav('/bookings')} onExpired={onExpired} />
                    <div className="row" style={{ marginTop: 12 }}>
                        <button className="btn ghost" onClick={cancel}>Hủy giữ ghế</button>
                    </div>
                </div>
            )}
        </>
    );
}
