import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/api";

function LoginPage() {
    const [form, setForm] = useState({email: "", password: ""});
    const [error, setError] = useState("");
    const navigate = useNavigate();

    const handleChange = (e) => {
        setForm({ ...form, [e.target.name]: e.target.value});
    };

    const handleSubmit = ()  => {
        setError("");
        api.post("/auth/login", form)
            .then(res => {
                localStorage.setItem("token", res.data.token);
                localStorage.setItem("role", res.data.role);
                window.location.href = "/employees";
            })
            .catch(() => setError("Nepareizs e-pasts vai parole!"));
    };

    return (
        <div>
            <h1>Pieslēgties</h1>
            <div>
                <input
                type="email"
                name="email"
                placeholder="E-pasts"
                value={form.email}
                onChange={handleChange}
                />
            </div>
            <div>
                <input
                type="password"
                name="password"
                placeholder="Parole"
                value={form.password}
                onChange={handleChange}
                />
            </div>
            <button onClick={handleSubmit}>Pieslēgties</button>
            {error && <p style={{color: "red"}}>{error}</p>}
        </div>
    );
}

export default LoginPage;

