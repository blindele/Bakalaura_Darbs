import {useState} from "react";
import api from "../api/api"

function GeneratePage() {
    const [form, setForm] = useState({
        year: 2026,
        month: 4,
        shiftStart: "11:00",
        shiftEnd: "20:00",
        requiredEmployees: 14,
        minEmployeesPerDay: 8,
    });

    const [message, setMessage] = useState("");
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        setForm({ ...form, [e.target.name]: e.target.value });
    };

    const handleSubmit = (e) => {
        setLoading(true);
        setMessage("");
        api.post("/schedules/generate", {
            ...form,
            year: Number(form.year),
            month: Number(form.month),
            requiredEmployees: Number(form.requiredEmployees),
            minEmployeesPerDay: Number(form.minEmployeesPerDay),
        })
        .then(() => setMessage("Grafiks izveidots!"))
        .catch((err) => setMessage("Kļūda " + (err.response?.data || err.message)))
        .finally(() => setLoading(false));
    };


    return (
        <div>
            <h1>Ģenerēt grafiku</h1>
            <form onSubmit={handleSubmit}>
                <div>
                    <label>Gads:</label>
                    <select name="year" value={form.year} onChange={handleChange}>
                        <option value={2026}>2026</option>
                        <option value={2027}>2027</option>
                    </select>
                </div>
                <div>
                    <label>Mēnesis:</label>
                    <select name="month" value={form.month} onChange={handleChange}>
                        {Array.from({length:12}, (_, i) => (
                            <option key={i + 1} value={i + 1}>{i+1}. mēnesis</option>
                        ))}
                    </select>
                </div>
                <div>
                    <label>Maiņas sākums</label>
                    <input
                    type="time"
                    name="shiftStart"
                    value={form.shiftStart}
                    onChange={handleChange}
                    required
                    />
                </div>
                <div>
                    <label>Maiņas beigas</label>
                    <input
                    type="time"
                    name="shiftEnd"
                    value={form.shiftEnd}
                    onChange={handleChange}
                    required
                    />
                </div>
                <div>
                    <label>Nepieciešamie darbinieki</label>
                    <input
                    type="number"
                    name="requiredEmployees"
                    value={form.requiredEmployees}
                    onChange={handleChange}
                    min={1}
                    required
                    />
                </div>
                <div>
                    <label>Minimālais darbinieku skaits dienā:</label>
                    <input
                        type="number"
                        name="minEmployeesPerDay"
                        value={form.minEmployeesPerDay}
                        onChange={handleChange}
                        min={1}
                        required
                    />
                </div>
                <button type="button" onClick={handleSubmit} disabled={loading}>
                    {loading ? "Ģenerē..." : "Ģenerēt"}
                </button>
            </form>
            {message && <p>{message}</p>}
        </div>
    );
}

export default GeneratePage;