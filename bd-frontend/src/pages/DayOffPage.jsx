import {useEffect, useState} from "react";
import api from "../api/api";

function DayOffPage() {
    const role = localStorage.getItem("role");
    const [requests, setRequests] = useState([]);
    const [date, setDate] = useState("");
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    useEffect(() => {
        fetchRequests();
    }, []);

    const fetchRequests = () => {
        if (role === "ADMIN") {
            api.get("/dayoff/pending").then(res => setRequests(res.data));
        } else {
            api.get("/dayoff/my-requests").then(res => setRequests(res.data));
        }
    };

    const handleSubmit = () => {
        setMessage("");
        setError("");
        if (!date) {
            setError("Lūdzu izvēlaties datumu");
            return;
        }
        api.post("/dayoff", {date})
            .then(() => {
                setMessage("Pieprasījums iesniegts");
                setDate("");
                fetchRequests();
            })
            .catch(() => setError("Pieprasījums diemžēl nav izdevies"));
    };

    const handleApprove = (id) => {
        api.put(`/dayoff/${id}`, {status : "APPROVED"})
            .then(() => fetchRequests());
    };

    const handleReject = (id) => {
        api.put(`/dayoff/${id}`, {status : "REJECTED"})
            .then(() => fetchRequests());
    };

    const handleDelete = (id) => {
        api.delete(`/dayoff/${id}`)
            .then(() => fetchRequests());
    }

    const getStatusLabel = (status) => {
        if(status === "PENDING") return "Gaida apstiprinājumu";
        if(status === "APPROVED") return "Apstiprināts";
        if(status === "REJECTED") return "Noraidīts";
        return status;
    };


    return (
        <div>
            <h1>Brīvdienu pieprasījumi</h1>


            {role !== "ADMIN" && (
                <div>
                    <h2>Veidot pieprasījumu</h2>
                    <input
                        type="date"
                        value={date}
                        onChange={e => setDate(e.target.value)}
                    />
                    <button onClick={handleSubmit}>Iesniegt</button>
                    {message && <p style={{color : "green"}}>{message}</p>}
                    {error && <p style={{color: "red"}}>{error}</p>}
                </div>
            )}

            <h2>Brīvdienu pieprasījumi</h2>

            {requests.length === 0 ? (
                <p>Nav pieprasījumu</p>
            ) : (
                <table border="1" style={{borderCollapse: "collapse"}}>
                    <thead>
                        <tr>
                            {role === "ADMIN" && (
                                <th style={{padding: "4px 8px"}}>Darbinieks</th>
                            )}
                            <th style={{padding: "4px 8px"}}>Datums</th>
                            <th style={{padding: "4xp 8px"}}>Statuss</th>
                            <th style={{padding: "4px 8px"}}>Darbība</th>
                        </tr>
                    </thead>
                    <tbody>
                        {requests.map(req => (
                            <tr key={req.id}>
                                {role === "ADMIN" && (
                                    <td style={{padding : "4px 8px"}}>
                                        {req.employee.name} {req.employee.surname}
                                    </td>
                                )}
                                <td style={{padding: "4px 8px"}}>{req.date}</td>
                                <td style={{padding: "4px 8px"}}>{getStatusLabel(req.status)}</td>
                                <td style={{padding: "4px 8px"}}>
                                    {role === "ADMIN" ? (
                                        <>
                                            <button onClick={() => handleApprove(req.id)}>
                                                Apstiprināt
                                            </button>
                                            <button
                                                onClick={() => handleReject(req.id)}
                                                style={{marginLeft: "0.5rem"}}
                                            >
                                                Noraidīt
                                            </button>
                                        </>
                                    ) : (
                                        req.status === "PENDING" && (
                                            <button onClick={() => handleDelete(req.id)}>
                                                Dzēst
                                            </button>
                                        )
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}

        </div>
    );

}

export default DayOffPage