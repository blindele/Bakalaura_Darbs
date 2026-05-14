import { BrowserRouter, Routes, Route, Link, Navigate } from "react-router-dom";
import EmployeesPage from "./pages/EmployeesPage";
import SchedulePage from "./pages/SchedulePage";
import GeneratePage from "./pages/GeneratePage";
import LoginPage from "./pages/LoginPage";
import ProtectedRoute from "./components/ProtectedRoute";
import { useState } from "react";
import ProfilePage from "./pages/ProfilePage";
import DayOffPage from "./pages/DayOffPage";

function App() {

  const [token, setToken] = useState (localStorage.getItem("token"));
  const [role, setRole] = useState (localStorage.getItem("role"));

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    setToken(null);
    setRole(null);
    window.location.href = "/login"
  };

  return (
    <BrowserRouter>
    {token && (
      <nav>
          <div className="container" style={{display: "flex", alignItems: "center", gap: "20px"}}>
        {role === "ADMIN" && (
        <>
        <Link to="/employees">Darbinieki</Link>
        <Link to="/generate">Ģenerēt grafiku</Link>
        </>
      )}
          <Link to="/schedule">Grafiks</Link>
          <Link to="/profile">Profils</Link>
          <Link to="/dayoff">Brīvdienu pieprasījumi</Link>
          <button onClick={handleLogout}>Iziet</button>
          </div>
        </nav>
    )}
      <Routes>
        <Route path="/login" element={<LoginPage/>} />
        <Route path="/employees" element={<ProtectedRoute adminOnly={true}><EmployeesPage /></ProtectedRoute>} />
        <Route path="/schedule" element= {<ProtectedRoute><SchedulePage /></ProtectedRoute>} />
        <Route path="/generate" element={<ProtectedRoute adminOnly={true}><GeneratePage/></ProtectedRoute>} />
        <Route path="/profile" element={<ProtectedRoute><ProfilePage/></ProtectedRoute>}/>
        <Route path="/dayoff" element={<ProtectedRoute><DayOffPage/></ProtectedRoute>}/>
        <Route path="/" element={<Navigate to="/login" />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;