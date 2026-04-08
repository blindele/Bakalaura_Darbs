import { useEffect, useState } from "react";
import api from "../api/api";

function EmployeesPage() {
  const [employees, setEmployees] = useState([]);
  const [form, setForm] = useState({
    name: "",
    surname: "",
    birthDate: "",
    gender: ""
  });

  useEffect(() => {
    fetchEmployees();
  }, []);

  const fetchEmployees = () => {
    api.get("/employees").then((res) => setEmployees(res.data));
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    api.post("/employees", form).then(() => {
      setForm({ name: "", surname: "", birthDate: "" });
      fetchEmployees();
    });
  };

  const handleDelete = (id) => {
    api.delete(`/employees/${id}`).then(() => fetchEmployees());
  };

  return (
    <div>
      <h1>Darbinieki</h1>

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

        <button type="submit">Pievienot</button>
      </form>

      <h2>Darbinieku saraksts</h2>
      <table border="1">
        <thead>
          <tr>
            <th>Vārds</th>
            <th>Uzvārds</th>
            <th>Dzimšanas datums</th>
            <th>Dzimums</th>
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