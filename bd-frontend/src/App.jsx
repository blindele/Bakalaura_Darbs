import { BrowserRouter, Routes, Route, Link } from "react-router-dom";
import EmployeesPage from "./pages/EmployeesPage";

function App() {
  return (
    <BrowserRouter>
      <nav>
        <Link to="/employees">Darbinieki</Link>
      </nav>
      <Routes>
        <Route path="/employees" element={<EmployeesPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;