import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { errMsg } from '../api';

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function Login() {
    const nav = useNavigate();
    const [mode, setMode] = useState('login');
    const [f, setF] = useState({ username: '', email: '', password: '', confirmPassword: '' });
    const [msg, setMsg] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const set = (k) => (e) => {
        setF((current) => ({ ...current, [k]: e.target.value }));
        setMsg('');
    };

    const errors = useMemo(() => ({
        username: f.username.trim().length < 3 ? 'Tài khoản phải có ít nhất 3 ký tự.' : '',
        email: !emailPattern.test(f.email.trim()) ? 'Email không đúng định dạng.' : '',
        password: f.password.length < 6 ? 'Mật khẩu phải có ít nhất 6 ký tự.' : '',
        confirmPassword: f.confirmPassword !== f.password ? 'Mật khẩu xác nhận không khớp.' : '',
    }), [f]);

    const submit = async (e) => {
        e.preventDefault();
        setMsg('');
        if (mode === 'register') {
            const firstError = errors.username || errors.email || errors.password || errors.confirmPassword;
            if (firstError) {
                setMsg(firstError);
                return;
            }
        } else if (!f.username.trim() || !f.password) {
            setMsg('Vui lòng nhập tài khoản và mật khẩu.');
            return;
        }

        try {
            if (mode === 'register') {
                await api.post('/api/auth/register', {
                    username: f.username.trim(), email: f.email.trim(), password: f.password,
                    confirmPassword: f.confirmPassword,
                });
                setMode('login');
                setF((current) => ({ ...current, email: '', password: '', confirmPassword: '' }));
                setMsg('Đăng ký thành công, hãy đăng nhập.');
                return;
            }
            const { data } = await api.post('/api/auth/login', { username: f.username.trim(), password: f.password });
            localStorage.setItem('token', data.token);
            localStorage.setItem('user', JSON.stringify(data));
            nav('/');
        } catch (err) {
            setMsg(errMsg(err));
        }
    };

    const renderPassword = (field, label, visible, toggle, error) => (
        <div className="password-field">
            <input
                type={visible ? 'text' : 'password'}
                placeholder={label}
                value={f[field]}
                onChange={set(field)}
                required
                aria-invalid={Boolean(error)}
            />
            <button className="password-toggle" type="button" onClick={() => toggle(!visible)}
                aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} title={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}>
                <svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="1.8">
                    {visible ? <><path d="M3 3l18 18"/><path d="M10.6 10.6a2 2 0 0 0 2.8 2.8"/><path d="M9.9 5.2A10.8 10.8 0 0 1 12 5c5 0 8.5 4.5 9.5 7-.4 1-1.2 2.1-2.3 3.1M6.2 6.2C4.3 7.5 3 9.4 2.5 12c.5 2.5 4 7 9.5 7 1 0 2-.2 2.8-.5"/></>
                        : <><path d="M2.5 12S6 5 12 5s9.5 7 9.5 7-3.5 7-9.5 7-9.5-7-9.5-7Z"/><circle cx="12" cy="12" r="3"/></>}
                </svg>
            </button>
        </div>
    );

    return (
        <form className="card form" onSubmit={submit} noValidate>
            <h2>{mode === 'login' ? 'Đăng nhập' : 'Đăng ký'}</h2>
            <div className="field-group">
                <input placeholder="Tên đăng nhập" value={f.username} onChange={set('username')} required aria-invalid={mode === 'register' && Boolean(errors.username)} />
                {mode === 'register' && f.username.length > 0 && <small className={errors.username ? 'field-error' : 'field-ok'}>{errors.username || 'Tài khoản hợp lệ.'}</small>}
            </div>
            {mode === 'register' && <div className="field-group">
                <input type="email" placeholder="Email" value={f.email} onChange={set('email')} required aria-invalid={Boolean(errors.email)} />
                {f.email.length > 0 && <small className={errors.email ? 'field-error' : 'field-ok'}>{errors.email || 'Email hợp lệ.'}</small>}
            </div>}
            <div className="field-group">
                {renderPassword('password', 'Mật khẩu (từ 6 ký tự)', showPassword, setShowPassword, mode === 'register' ? errors.password : '')}
                {mode === 'register' && f.password.length > 0 && <small className={errors.password ? 'field-error' : 'field-ok'}>{errors.password || 'Mật khẩu hợp lệ.'}</small>}
            </div>
            {mode === 'register' && <div className="field-group">
                {renderPassword('confirmPassword', 'Xác nhận mật khẩu', showConfirmPassword, setShowConfirmPassword, errors.confirmPassword)}
                {f.confirmPassword.length > 0 && <small className={errors.confirmPassword ? 'field-error' : 'field-ok'}>{errors.confirmPassword || 'Mật khẩu xác nhận khớp.'}</small>}
            </div>}
            {msg && <p className="msg" role="alert">{msg}</p>}
            <button className="btn">{mode === 'login' ? 'Đăng nhập' : 'Đăng ký'}</button>
            <a className="link" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setMsg(''); }}>
                {mode === 'login' ? 'Chưa có tài khoản? Đăng ký' : 'Đã có tài khoản? Đăng nhập'}
            </a>
        </form>
    );
}
