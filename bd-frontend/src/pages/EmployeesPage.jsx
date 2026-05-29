import { useEffect, useRef, useState } from "react";
import api from "../api/api";

const MONTH_NAMES = [
  "Janvāris", "Februāris", "Marts", "Aprīlis", "Maijs", "Jūnijs",
  "Jūlijs", "Augusts", "Septembris", "Oktobris", "Novembris", "Decembris"
];
function EmployeesPage() {
  const [employees, setEmployees] = useState([]);
  const [pendingDeactivation, setPendingDeactivation] = useState([]);
  const [createdUsers, setCreatedUsers] = useState([]);
  const [bulkErrors, setBulkErrors] = useState([]);
  const [error, setError] = useState("");
  const fileInput = useRef(null);
  const [form, setForm] = useState({
    name: "",
    surname: "",
    birthDate: "",
    gender: "",
    email: "",
    permanent: true,
    workingMonths: [],
  });

  useEffect(() => {
    fetchEmployees();
    fetchPendingDeactivation();
  }, []);

  const fetchEmployees = () => {
    api.get("/employees").then((res) => setEmployees(res.data));
  };

  const fetchPendingDeactivation = () => {
    api.get("/employees/pending-deactivation")
    .then((res) => setPendingDeactivation(res.data))
    .catch(() => setPendingDeactivation([]));
  };

  const handleChange = (e) => {
    const {name, value, type, checked} = e.target;
    if (type === "checkbox" && name === "permanent") {
      setForm({
        ...form,
        permanent: checked,
        workingMonths: checked ? [] : form.workingMonths,
      });
    } else {
      setForm({ ...form, [e.target.name]: e.target.value });
    }
  };

  const handleMonthToggle = (month) => {
    setForm((prev) => {
      const exists = prev.workingMonths.includes(month);
      const updated = exists
      ? prev.workingMonths.filter((m) => m !== month)
      : [...prev.workingMonths, month].sort((a,b) => a-b);
      return { ...prev, workingMonths: updated};
    });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setError("");

    if(!form.permanent && form.workingMonths.length === 0) {
      setError("Nepieciešams norādīt vismaz vienu mēnesi");
      return;
    }


    api.post(`/employees?email=${form.email}`, {
      name: form.name,
      surname: form.surname,
      birthDate: form.birthDate,
      gender: form.gender,
      permanent: form.permanent,
      workingMonths: form.workingMonths,
    }).then((res) => {
      setCreatedUsers((prev) => [...prev, res.data]); 
      setForm({name: "", surname: "", birthDate: "", gender: "", email: "", permanent: true, workingMonths: []});
      fetchEmployees();
      fetchPendingDeactivation();
    }).catch((err) => {
    setError(err.response?.data?.message || "Radās kļūda veidojot darbinieku");
  });
};

  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if(!file) return;
    setError("");
    setBulkErrors([]);

    try {
      const data = JSON.parse(await file.text());
      const res = await api.post("/employees/bulk", data);
      setCreatedUsers((prev) => [...prev, ...res.data.successful]);
      setBulkErrors(res.data.failed || []);
      fetchEmployees();
      fetchPendingDeactivation();
    } catch (ex) {
      setError(ex.message || "Radās kļūda, pārbaudat failu un mēģinat vēlreiz");
    } finally {
      if (fileInput.current) fileInput.current.value = "";
    }
  };

  const downloadCSV = () => {
    if (createdUsers.length === 0) return;
    const rows = [["Vārds", "Uzvārds", "E-pasts", "Parole"]];
    createdUsers.forEach((c) =>
      rows.push([c.employee?.name, c.employee?.surname, c.email, c.password])
    );
    const csv = rows.map((r) =>

      r.map((cell) => `"${String(cell || "").replace(/"/g, '""')}"`).join(",")
    ).join("\n");

    const blob = new Blob(["\uFEFF" + csv], {type: "text/csv;charset=utf-8;"});
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `darbinieki_${new Date().toISOString().split("T")[0]}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const clearCredentials = () => {
    if (createdUsers.length === 0) return;
    if (!window.confirm(
      "Pēc apstiprināšanas dati vairs nebuus pieejami. Turpināt?"
    )) return;
    setCreatedUsers([]);
    setBulkErrors([]);
  };

  const handleDelete = (id) => {
    api.delete(`/employees/${id}`).then(() => {
      fetchEmployees();
      fetchPendingDeactivation();
    });
  };

  const handleDeactivate = (id) => {
    api.post(`/employees/${id}/deactivate`).then(() => {
      fetchEmployees();
      fetchPendingDeactivation();
    });
  };

  return (
    <div className="container">
      <h1>Darbinieki</h1>

      {pendingDeactivation.length > 0 && (
        <div style={{
          background: "#fff4e5", border: "1px solid #f0b357",
          padding: "1rem", borderRadius: "6px", marginBottom: "1rem"
        }}>
          <h2 style={{ marginTop: 0 }}>
            Sezonas darbinieki, kuri beiguši darbu ({pendingDeactivation.length})
          </h2>
          <table border="1" style={{ width: "100%" }}>
            <thead>
              <tr>
                <th>Vārds</th><th>Uzvārds</th><th>Strādāja mēnešos</th><th>Darbības</th>
              </tr>
            </thead>
            <tbody>
              {pendingDeactivation.map((emp) => (
                <tr key={emp.id}>
                  <td>{emp.name}</td>
                  <td>{emp.surname}</td>
                  <td>
                    {[...(emp.workingMonths || [])]
                      .sort((a, b) => a - b)
                      .map((m) => MONTH_NAMES[m - 1])
                      .join(", ")}
                  </td>
                  <td>
                    <button onClick={() => handleDeactivate(emp.id)}>Deaktivizēt</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h2>Pievienot vienu darbinieku</h2>
      <form onSubmit={handleSubmit}>
        <input name="name" placeholder="Vārds" value={form.name}
          onChange={handleChange} required />
        <input name="surname" placeholder="Uzvārds" value={form.surname}
          onChange={handleChange} required />
        <input name="email" type="email" placeholder="E-pasts" value={form.email}
          onChange={handleChange} required />
        <input name="birthDate" type="date" value={form.birthDate}
          onChange={handleChange} required />
        <select name="gender" value={form.gender} onChange={handleChange} required>
          <option value="">Izvēlaties dzimumu</option>
          <option value="MALE">Vīrietis</option>
          <option value="FEMALE">Sieviete</option>
        </select>

        <div style={{ margin: "0.75rem 0" }}>
          <label>
            <input type="checkbox" name="permanent"
              checked={form.permanent} onChange={handleChange} />
            {" "}Pastāvīgs darbinieks
          </label>
        </div>

        {!form.permanent && (
          <div style={{
            border: "1px solid #ccc", padding: "0.75rem",
            borderRadius: "6px", marginBottom: "0.75rem"
          }}>
            <div style={{ marginBottom: "0.5rem" }}>
              Atzīmē mēnešus, kuros darbinieks strādās:
            </div>
            <div style={{
              display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "0.25rem"
            }}>
              {MONTH_NAMES.map((monthName, idx) => {
                const monthNumber = idx + 1;
                return (
                  <label key={monthNumber}>
                    <input type="checkbox"
                      checked={form.workingMonths.includes(monthNumber)}
                      onChange={() => handleMonthToggle(monthNumber)} />
                    {" "}{monthName}
                  </label>
                );
              })}
            </div>
          </div>
        )}

        {error && (
          <div style={{ color: "red", marginBottom: "0.75rem" }}>{error}</div>
        )}

        <button type="submit">Pievienot</button>
      </form>

      <h2 style={{ marginTop: "2rem" }}>Pievienot vairākus no JSON faila</h2>
      <div style={{
        border: "1px dashed #aaa", padding: "1rem", borderRadius: "6px"
      }}>
        <p style={{ marginTop: 0 }}>
          Augšupielādē JSON failu ar darbinieku sarakstu.
        </p>
        <input
          ref={fileInput}
          type="file"
          accept=".json,application/json"
          onChange={handleFileUpload}
        />
      </div>

      {createdUsers.length > 0 && (
        <div style={{
          background: "#e6f7e6", border: "1px solid #5cb85c",
          padding: "1rem", borderRadius: "6px", marginTop: "1.5rem"
        }}>
          <div style={{
            display: "flex", justifyContent: "space-between", alignItems: "center"
          }}>
            <h2 style={{ margin: 0 }}>
              Tikko izveidotie darbinieki ({createdUsers.length})
            </h2>
            <div>
              <button onClick={downloadCSV}>Lejupielādēt CSV</button>
              {" "}
              <button onClick={clearCredentials}>Notīrīt sarakstu</button>
            </div>
          </div>
          <p style={{ color: "#c00", fontSize: "0.9rem" }}>
            Lejupielādē sarakstu, lai nezaudētu darbinieku ielogošanās informāciju.
          </p>
          <table border="1" style={{ width: "100%" }}>
            <thead>
              <tr>
                <th>Vārds</th><th>Uzvārds</th><th>E-pasts</th><th>Parole</th>
              </tr>
            </thead>
            <tbody>
              {createdUsers.map((c, idx) => (
                <tr key={idx}>
                  <td>{c.employee?.name}</td>
                  <td>{c.employee?.surname}</td>
                  <td>{c.email}</td>
                  <td style={{ fontFamily: "monospace" }}>{c.password}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {bulkErrors.length > 0 && (
        <div style={{
          background: "#fbeaea", border: "1px solid #d9534f",
          padding: "1rem", borderRadius: "6px", marginTop: "1rem"
        }}>
          <h2 style={{ marginTop: 0 }}>
            Neizdevās izveidot ({bulkErrors.length})
          </h2>
          <table border="1" style={{ width: "100%" }}>
            <thead>
              <tr>
                <th>Vārds</th><th>Uzvārds</th><th>E-pasts</th><th>Kļūda</th>
              </tr>
            </thead>
            <tbody>
              {bulkErrors.map((err, idx) => (
                <tr key={idx}>
                  <td>{err.name}</td>
                  <td>{err.surname}</td>
                  <td>{err.email}</td>
                  <td style={{ color: "#c00" }}>{err.error}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h2 style={{ marginTop: "2rem" }}>Darbinieku saraksts</h2>
      <table border="1">
        <thead>
          <tr>
            <th>Vārds</th><th>Uzvārds</th><th>Dzimšanas datums</th>
            <th>Dzimums</th><th>Tips</th><th>Darbības</th>
          </tr>
        </thead>
        <tbody>
          {employees.map((emp) => (
            <tr key={emp.id}>
              <td>{emp.name}</td>
              <td>{emp.surname}</td>
              <td>{emp.birthDate}</td>
              <td>{emp.gender === "MALE" ? "Vīrietis" : "Sieviete"}</td>
              <td>
                {emp.permanent
                  ? "Patstāvīgs"
                  : `Sezonas (${[...(emp.workingMonths || [])]
                      .sort((a, b) => a - b)
                      .map((m) => MONTH_NAMES[m - 1].substring(0, 3))
                      .join(", ")})`}
              </td>
              <td>
                <button onClick={() => handleDelete(emp.id)}>Dzēst</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default EmployeesPage;