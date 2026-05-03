import { BrowserRouter, Routes, Route, Link } from "react-router-dom";
import EmployeesPage from "./pages/EmployeesPage";
import SchedulePage from "./pages/SchedulePage";
import GeneratePage from "./pages/GeneratePage";
import LoginPage from "./pages/LoginPage";
import ProtectedRoute from "./components/ProtectedRoute";

function App() {

  const token = localStorage.getItem("token");

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    window.location.href = "/login"
  };

  return (
    <BrowserRouter>
    {token && (
      <nav>
        <Link to="/employees">Darbinieki</Link>
        <Link to="/schedule">Grafiks</Link>
        <Link to="/generate">Ģenerēt grafiku</Link>
        <button onClick={handleLogout}>Iziet</button>
      </nav>
      )}
      <Routes>
        <Route path="/login" element={<LoginPage/>} />
        <Route path="/employees" element={<ProtectedRoute><EmployeesPage /></ProtectedRoute>} />
        <Route path="/schedule" element= {<ProtectedRoute><SchedulePage /></ProtectedRoute>} />
        <Route path="/generate" element={<ProtectedRoute><GeneratePage/></ProtectedRoute>} />
        <Route path="/" element={<LoginPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;