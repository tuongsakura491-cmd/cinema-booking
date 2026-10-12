import { useState } from 'react';
import api, { errMsg, money } from '../api';

const METHODS = [
    { value: 'CARD', label: 'Thẻ ngân hàng' },
    { value: 'EWALLET', label: 'Ví điện tử' },
    { value: 'BANK_TRANSFER', label: 'Chuyển khoản' },
];

/**
 * Khung thanh toán dùng chung (trang chọn ghế và trang Vé của tôi).
 * Gọi POST /api/bookings/{id}/confirm kèm phương thức. Số tiền KHÔNG gửi từ client.
 */
export default function PayPanel({ booking, onPaid, onExpired }) {
    const [method, setMethod] = useState('CARD');
    const [last4, setLast4] = useState('');
    const [busy, setBusy] = useState(false);
    const [msg, setMsg] = useState('');

    const cardErr = method === 'CARD' && !/^\d{4}$/.test(last4);

    const pay = async () => {
        if (cardErr) {
            setMsg('Nhập đúng 4 chữ số cuối của thẻ.');
            return;
        }
        setBusy(true);
        setMsg('');
        try {
            await api.post(`/api/bookings/${booking.id}/confirm`, {
                method,
                cardLast4: method === 'CARD' ? last4 : undefined,
            });
            onPaid();
        } catch (e) {
            if (e.response?.status === 410) {
                onExpired();
            } else if (e.response?.status === 402) {
                setMsg(`Thanh toán thất bại: ${errMsg(e)}. Bạn có thể thử lại trong thời gian giữ ghế.`);
            } else {
                setMsg(errMsg(e));
            }
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="pay-panel">
            <div className="pay-methods" role="radiogroup" aria-label="Phương thức thanh toán">
                {METHODS.map((m) => (
                    <button key={m.value} type="button" role="radio" aria-checked={method === m.value}
                            className={`pay-method ${method === m.value ? 'active' : ''}`}
                            onClick={() => { setMethod(m.value); setMsg(''); }}>
                        {m.label}
                    </button>
                ))}
            </div>

            {method === 'CARD' && (
                <div className="pay-field">
                    <input inputMode="numeric" maxLength={4} placeholder="4 số cuối của thẻ"
                           value={last4} onChange={(e) => setLast4(e.target.value.replace(/\D/g, ''))} />
                    <small className="muted-note">Mô phỏng: nhập 0000 để thử trường hợp thẻ bị từ chối.</small>
                </div>
            )}

            {msg && <p className="msg">{msg}</p>}

            <button className="btn" disabled={busy} onClick={pay}>
                {busy ? 'Đang thanh toán...' : `Thanh toán ${money(booking.totalPrice)}`}
            </button>
        </div>
    );
}
