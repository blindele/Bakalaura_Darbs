import { useEffect, useState } from "react";
import api from "../api/api";

const MONTH_NAMES = [
  "Janvāris", "Februāris", "Marts", "Aprīlis", "Maijs", "Jūnijs",
  "Jūlijs", "Augusts", "Septembris", "Oktobris", "Novembris", "Decembris"
];

function EmployeesPage() {
  const [employees, setEmployees] = useState([]);
  const [pendingDeactivation, setPendingDeactivation] = useState([]);
  const [form, setForm] = useState({
    name: "",
    surname: "",
    birthDate: "",
    gender: "",
    email: "",
    permanent: true,
    workingMonths: [],
  });

  const [newEmployeeInfo, setNewEmployeeInfo] = useState(null);
  const [error, setError] = useState("");

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
      setNewEmployeeInfo(res.data);
      setForm({name: "", surname: "", birthDate: "", gender: "", email: "", permanent: true, workingMonths: []});
      fetchEmployees();
      fetchPendingDeactivation();
    }).catch((err) => {
    setError(err.response?.data?.message || "Radās kļūda");
  });
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
          <h2 style={{marginTop: 0}}>
            Sezonas darbinieki, ko nepieciešams deaktivizēt ({pendingDeactivation.length})
          </h2>
          <p style={{marginTop: 0, fontSize: "0.9rem"}}>
            Darbinieki savu laiku ir nostrādājuši un nepieciešamības gadījumā kontus var deaktivizēt
          </p>
          <table border="1" style={{width: "100%"}}>
            <thead>
              <tr>
                <th>Vārds</th>
                <th>Uzvārds</th>
                <th>Mēneši</th>
                <th>Darbības</th>
              </tr>
            </thead>
            <tbody>
              {pendingDeactivation.map((emp) => (
                <tr key={emp.id}>
                  <td>{emp.name}</td>
                  <td>{emp.surname}</td>
                  <td>
                    {[...(emp.workingMonths || [])]
                      .sort((a,b) => a-b)
                      .map((map) => MONTH_NAMES[m-1])
                      .join(", ")}
                  </td>
                  <td>
                    <button onClick={() => handleDeactivate(emp.id)}>
                      Deaktivizēt
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h2>Pievienot darbinieku</h2>
      <form onSubmit={handleSubmit}>
        <input
          name="name"
          placeholder="Vārds"
          value={form.name}
          onChange={handleChange}
          required
        />
        <input
          name="surname"
          placeholder="Uzvārds"
          value={form.surname}
          onChange={handleChange}
          required
        />
        <input
          name="email"
          type="email"
          placeholder="E-pasts"
          value={form.email}
          onChange={handleChange}
          required
        />
        <input
          name="birthDate"
          type="date"
          value={form.birthDate}
          onChange={handleChange}
          required
        />
        <select name="gender" value={form.gender} onChange={handleChange} required>
        <option value="">Izvēlies dzimumu</option>
        <option value="MALE">Vīrietis</option>
        <option value="FEMALE">Sieviete</option>
        </select>

        <div style={{margin : "0.75rem 0"}}>
          <label>
            <input
              type="checkbox"
              name="permanent"
              checked={form.permanent}
              onChange={handleChange}
              />
              {" "} Patstāvīgs darbinieks
          </label>
        </div>

        {!form.permanent && (
          <div style={{
            border: "1px solid #ccc", padding: "0.75rem",
            borderRadius: "6px", marginBottom: "0.75rem"
          }}>
            <div style={{marginBottom: "0.5rem"}}>
              Mēneši, kuros darbinieks strādās:
            </div>
            <div style={{
              display: "grid",
              gridTemplateColumns: "repeat (4,1fr)",
              gap: "0.25rem"
            }}>
              {MONTH_NAMES.map((monthName, idx) => {
                const monthNumber = idx + 1;
                return (
                  <label key={monthNumber}>
                    <input
                      type="checkbox"
                      checked={form.workingMonths.includes(monthNumber)}
                      onChange={() => handleMonthToggle(monthNumber)}
                      />
                      {" "}{monthName}
                  </label>
                );
              })}
            </div>
          </div>
        )}

        {error && (
          <div style={{ color: "red", marginBottom: "0.75rem"}}>{error}</div>
        )}

        <button type="submit">Pievienot</button>
      </form>

      {newEmployeeInfo && (
        <div style={{
          position: "fixed", top: 0, left: 0, right: 0, bottom: 0,
          background: "rgba(0,0,0,0.5)",
          display: "flex", alignItems: "center", justifyContent: "center" 
        }}>
          <div style={{
            background: "white", padding: "2rem", borderRadius: "8px",
            minWidth: "300px"
          }}>

            <h2>Darbinieks izveidots!</h2>
            <p style={{color: "red"}}>
                Pēc aizvēršanas informācija vairs nebūs pieejama!
            </p>
            <p><strong>E-pasts:</strong> {newEmployeeInfo.email}</p>
            <p><strong>Parole:</strong>{newEmployeeInfo.password}</p>
            <button onClick={() => setNewEmployeeInfo(null)}>
              Aizvērt
            </button>
          </div>
        </div>
      )}

      <h2>Darbinieku saraksts</h2>
      <table border="1">
        <thead>
          <tr>
            <th>Vārds</th>
            <th>Uzvārds</th>
            <th>Dzimšanas datums</th>
            <th>Dzimums</th>
            <th>Tips</th>
            <th>Darbības</th>
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
                    .sort((a,b) => a-b)
                    .map((m) => MONTH_NAMES[m-1].substring(0,3))
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