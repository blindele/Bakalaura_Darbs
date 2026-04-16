import { BrowserRouter, Routes, Route, Link } from "react-router-dom";
import EmployeesPage from "./pages/EmployeesPage";
import SchedulePage from "./pages/SchedulePage";

function App() {
  return (
    <BrowserRouter>
      <nav>
        <Link to="/employees">Darbinieki</Link>
        <Link to="/schedule">Grafiks</Link>
      </nav>
      <Routes>
        <Route path="/employees" element={<EmployeesPage />} />
        <Route path="/schedule" element= {<SchedulePage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;