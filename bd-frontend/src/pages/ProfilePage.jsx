import { useEffect, useState } from "react";
import api from "../api/api";

function ProfilePage() {
    const [profile, setProfile] = useState(null);
    const [showPasswordModal, setShowPasswordModal] = useState(false);
    const [form, setForm] = useState({
        oldPassword: "",
        newPassword: "",
        confirmPassword: "",
    });

    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    useEffect(() => {
        api.get("/auth/profile").then(res => setProfile(res.data));
    }, []);

    const handleChange = (e) => {
        setForm({ ...form, [e.target.name]: e.target.value});
    };

    const handlePasswordChange = () => {
        setMessage("");
        setError("");

        if (form.newPassword !== form.confirmPassword) {
            setError("Paroles nesakrīt!");
            return;
        }

        api.put("/auth/change-password", {
            oldPassword: form.oldPassword,
            newPassword: form.newPassword,
        })
        .then(() => {
            setMessage("Parole nomainīta!");
            setForm({oldPassword: "", newPassword: "", confirmPassword: ""});
            setTimeout(() => {
                setShowPasswordModal(false);
                setMessage("");
            }, 2000);
        })
        .catch(() => setError("Nepareiza esošā parole!"));
    };

    if (!profile) return <p>Ielādē...</p>


    return (

        <div className="container">
            <h1>Profils</h1>

            <p><strong>Vārds:</strong> {profile.name || "-"}</p>
            <p><strong>Uzvārds:</strong> {profile.surname || "-"}</p>
            <p><strong>E-pasts:</strong> {profile.email || "-"}</p>
            <p><strong>Loma:</strong> {profile.role === "ADMIN" ? "Administrators" : "Darbinieks"}</p>

            <button onClick={() => setShowPasswordModal(true)}>
                Mainīt paroli
            </button>

            {showPasswordModal && (
                <div style={{
                    position: "fixed", top: 0, left: 0, right: 0, bottom: 0,
                    background: "rgba(0,0,0,0.5)",
                    display: "flex", alignItems: "center", justifyContent: "center"
                }}>
                    <div style={{
                        background: "white", padding: "2rem", borderRadius: "8px",
                        minWidth: "300px"
                    }}>
                        <h2>Mainīt paroli</h2>
                        <div>
                            <input
                                type="password"
                                name="oldPassword"
                                placeholder="Esošā parole"
                                value={form.oldPassword}
                                onChange={handleChange}
                            />
                        </div>
                        <div>
                            <input
                            type="password"
                            name="newPassword"
                            placeholder="Jaunā parole"
                            value={form.newPassword}
                            onChange={handleChange}
                            />
                        </div>
                        <div>
                            <input
                            type="password"
                            name="confirmPassword"
                            placeholder="Atkārtot paroli"
                            value={form.confirmPassword}
                            onChange={handleChange}
                            />
                        </div>
                        {message && <p style={{color: "green"}}>{message}</p>}
                        {error && <p style={{color: "red"}}>{error}</p>}
                        <button onClick={handlePasswordChange}>Saglabāt</button>
                        <button onClick={() => {
                            setShowPasswordModal(false);
                            setForm({oldPassword: "", newPassword: "", confirmPassword: ""});
                            setMessage("");
                            setError("");
                        }}>Atcelt</button>
                    </div>
                </div>
            )}

        </div>
    );
}

export default ProfilePage;