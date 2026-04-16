import { BrowserRouter, Routes, Route, Link } from "react-router-dom";
import EmployeesPage from "./pages/EmployeesPage";
import SchedulePage from "./pages/SchedulePage";
import GeneratePage from "./pages/GeneratePage";

function App() {
  return (
    <BrowserRouter>
      <nav>
        <Link to="/employees">Darbinieki</Link>
        <Link to="/schedule">Grafiks</Link>
        <Link to="/generate">Ģenerēt grafiku</Link>
      </nav>
      <Routes>
        <Route path="/employees" element={<EmployeesPage />} />
        <Route path="/schedule" element= {<SchedulePage />} />
        <Route path="/generate" element={<GeneratePage/>} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;