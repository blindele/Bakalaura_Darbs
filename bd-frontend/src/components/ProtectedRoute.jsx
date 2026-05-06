import { Navigate } from "react-router-dom";

function ProtectedRoute({children, adminOnly = false}) {

    const token = localStorage.getItem("token");
    const role = localStorage.getItem("role");



    if(!token) {
        return <Navigate to="/login" />;
    }

    if(adminOnly && role !== "ADMIN"){
        return <Navigate to="/schedule" />;
    }

    return children;
}

export default ProtectedRoute;