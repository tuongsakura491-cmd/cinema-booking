import { useCallback, useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import api, { fmt } from '../api';

const POLL_MS = 30000; // hỏi server mỗi 30 giây khi đang mở app

export default function NotificationBell() {
    const [items, setItems] = useState([]);
    const [open, setOpen] = useState(false);
    const [dismissed, setDismissed] = useState([]); // banner đã đóng trong phiên này
    const box = useRef(null);

    const load = useCallback(async () => {
        try {
            const { data } = await api.get('/api/notifications');
            setItems(data);
        } catch {
            /* lỗi mạng: bỏ qua, lần poll sau thử lại. 401 đã được api.js xử lý */
        }
    }, []);

    useEffect(() => {
        load(); // vào app là lấy ngay
        const t = setInterval(load, POLL_MS);
        return () => clearInterval(t);
    }, [load]);

    // Bấm ra ngoài thì đóng danh sách
    useEffect(() => {
        const onDoc = (e) => { if (box.current && !box.current.contains(e.target)) setOpen(false); };
        document.addEventListener('mousedown', onDoc);
        return () => document.removeEventListener('mousedown', onDoc);
    }, []);

    const unread = items.filter((n) => !n.read);
    const banner = unread.find((n) => n.type === 'REMINDER' && !dismissed.includes(n.id));

    const markRead = async (id) => {
        setItems((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
        try { await api.put(`/api/notifications/${id}/read`); } catch { load(); }
    };
    const markAll = async () => {
        setItems((prev) => prev.map((n) => ({ ...n, read: true })));
        try { await api.put('/api/notifications/read-all'); } catch { load(); }
    };
    const closeBanner = (n) => {
        setDismissed((d) => [...d, n.id]);
        markRead(n.id);
    };

    return (
        <>
            <div className="bell" ref={box}>
                <button className="bell-btn" onClick={() => setOpen(!open)}
                        aria-label={`Thông báo, ${unread.length} chưa đọc`}>
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
                        <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                    </svg>
                    {unread.length > 0 && <span className="bell-badge">{unread.length > 9 ? '9+' : unread.length}</span>}
                </button>

                {open && (
                    <div className="bell-menu">
                        <div className="bell-head">
                            <strong>Thông báo</strong>
                            {unread.length > 0 && <a className="link" onClick={markAll}>Đánh dấu đã đọc</a>}
                        </div>
                        {items.length === 0 && <p className="bell-empty">Chưa có thông báo nào.</p>}
                        {items.map((n) => (
                            <div key={n.id} className={`bell-item ${n.read ? '' : 'unread'}`}
                                 onClick={() => !n.read && markRead(n.id)}>
                                <div className="bell-title">{n.title}</div>
                                <div className="bell-text">{n.content}</div>
                                <div className="bell-time">{fmt(n.createdAt)}</div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* Portal ra body vì .nav có backdrop-filter làm position:fixed bị lệch */}
            {banner && createPortal(
                <div className="reminder-banner" role="alert">
                    <div>
                        <strong>{banner.title}</strong>
                        <div>{banner.content}</div>
                    </div>
                    <button className="btn small" onClick={() => closeBanner(banner)}>Đã biết</button>
                </div>,
                document.body
            )}
        </>
    );
}
