import { useEffect, useState } from "react";
import api from "../api/api";

function SchedulePage(){
    const [year, setYear] = useState(2026);
    const [month, setMonth] = useState(4);
    const [data,setData] = useState(null);

    useEffect(() => {
        fetchSchedule();
    }, [year,month]);

    const fetchSchedule = () => {
        api.get(`/schedules/${year}/${month}/view`)
        .then(res => setData(res.data))
        .catch(() => setData(null));
    };

    const getShift = (employeeId, day) => {
        if(!data?.shifts) return null;
        return data.shifts.find(
            s => s.employee.id == employeeId &&
            s.start.startsWith(day)
        );
    };

    const formatTime = (shift) => {
        if(!shift) return "OFF";
        const start = shift.start.substring(11,16);
        const end = shift.end.substring(11,16);
        return `${start}-${end}`;
    };

    const getDayLabel = (day) => {
        const date = new Date(day);
        const dayNames = ["Sv", "P", "O", "T", "C", "Pk", "S"];
        return `${day.substring(8)} ${dayNames[date.getDay()]}`;
    };

    return (
        <div>
            <h1>Darba grafiks</h1>

            <div>
                <select value={year} onChange={e => setYear(Number(e.target.value))}>
                    <option value={2026}>2026</option>
                    <option value={2027}>2027</option>
                </select>
                <select value={month} onChange={e => setMonth(Number(e.target.value))}>
                    {Array.from({length: 12}, (_,i) =>(
                        <option key={i+1} value={i+1}>{i+1}. mēnesis</option>
                    ))}
                </select>
            </div>

            {!data || data.employees?.length === 0 ? (
                <p>Nav datu</p>
            ) : (
                <div style={{overflowX: "auto"}}>
                    <table border="1" style={{borderCollapse: "collapse", fontSize: "12px"}}>
                        <thead>
                            <tr>
                                <th style={{padding: "4px 8px"}}>Datums</th>
                                {data.employees.map(emp => (
                                    <th key={emp.id} style={{padding: "4px 8px"}}>
                                        {emp.name} {emp.surname}
                                    </th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {data.days?.map(day => (
                                <tr key={day}>
                                    <td style={{padding: "4px 8px", fontWeight: "bold"}}>
                                        {getDayLabel(day)}
                                    </td>
                                    {data.employees.map(emp => {
                                        const shift = getShift(emp.id,day);
                                        return(
                                            <td key={emp.id} style={{
                                                padding: "4px 8px",
                                                textAlign: "center",
                                                background: shift ? "#e8f5e9" : "#fafafa",
                                                color: shift ? "#2e7d32" : "#999"
                                            }}>
                                                {formatTime(shift)}
                                            </td>
                                        );
                                    })}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );

}

export default SchedulePage;
