import { useEffect, useState } from "react";
import api from "../api/api";

function SchedulePage(){
    const [year, setYear] = useState(2026);
    const [month, setMonth] = useState(4);
    const [data,setData] = useState(null);
    const [editModal, setEditModal] = useState(null);
    const [editForm, setEditForm] = useState(null);
    const role = localStorage.getItem("role");

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

    const handleCellClick = (employee, day) => {
        if (role !== "ADMIN") return;
        const shift = getShift(employee.id, day);
        setEditModal({employee, day, shift});
        setEditForm({
            start: shift ? shift.start.substring(11,16) : "11:00",
            end: shift ? shift.end.substring(11,16) : "20:00"
        });
    };

    const handleSave = () => {
        const {employee, day, shift} = editModal;

        if (shift) {
            api.put(`/schedules/shifts/${shift.id}`, {
                ...shift,
                start: `${day}T${editForm.start}:00`,
                end: `${day}T${editForm.end}:00`,
            }).then(() => {
                fetchSchedule();
                setEditModal(null);
            });
        } else {
            api.post("/schedules/shifts", {
                employee: {id: employee.id},
                schedule: {id: data.scheduleId},
                start: `${day}T${editForm.start}:00`,
                end: `${day}T${editForm.end}:00`
            }).then(() => {
                fetchSchedule();
                setEditModal(null);
            });
        }

    };

    const handleDelete = () => {
        const {shift} = editModal;
        if(!shift) {
            setEditModal(null);
            return;
        }
        api.delete(`/schedules/shifts/${shift.id}`).then(() => {
            fetchSchedule();
            setEditModal(null);
        });
    };

    const getTotalHours = (employeeId) => {
        if( !data?.shifts) return 0;
        const empShifts = data.shifts.filter(s => s.employee.id === employeeId);
        return empShifts.reduce((total, shift) => {
            const start = new Date(shift.start);
            const end = new Date(shift.end);
            return total + (end - start) / (1000 * 60 * 60);
        }, 0);
    };


    return (
        <div className="container">
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
                                            <td key={emp.id}
                                            onClick={() => handleCellClick(emp,day)}
                                            style={{
                                                padding: "4px 8px",
                                                textAlign: "center",
                                                background: shift ? "#e8f5e9" : "#fafafa",
                                                color: shift ? "#2e7d32" : "#999",
                                                cursor: role === "ADMIN" ? "pointer" : "default"
                                            }}>
                                                {formatTime(shift)}
                                            </td>
                                        );
                                    })}
                                </tr>
                            ))}
                        </tbody>
                        <tfoot>
                            <tr>
                                <td style={{padding: "4px 8px", fontWeight: "bold"}}>
                                    Stundas:
                                </td>
                                {data.employees.map(emp => (
                                    <td key={emp.id} style={{
                                        padding: "4px 8px",
                                        textAlign: "center",
                                        fontWeight: "bold",
                                        background: "#e8f5e9",
                                        color: "#2e7d32"
                                    }}>
                                        {getTotalHours(emp.id)}h
                                    </td>
                                ))}
                            </tr>
                        </tfoot>
                    </table>
                </div>
            )}

            {editModal && (
                <div style={{
                    position: "fixed", top: 0, left: 0, right: 0, bottom: 0,
                    background: "rgba(0,0,0,0.5)",
                    display: "flex", alignItems: "center", justifyContent: "center"
                }}>
                    <div style={{
                        background: "white", padding: "2rem",
                        borderRadius: "8px", minWidth: "300px"
                    }}>
                        <h2>
                            {editModal.employee.name} {editModal.employee.surname}
                            {" - "}{editModal.day}
                        </h2>
                        <div>
                            <label>Sākums</label>
                            <input
                                type="time"
                                value={editForm.start}
                                onChange={e => setEditForm({...editForm, start: e.target.value})}
                            />
                        </div>
                        <div>
                            <label>Beigas:</label>
                            <input
                                type="time"
                                value={editForm.end}
                                onChange={e => setEditForm({...editForm, end: e.target.value})}
                            />
                        </div>
                        <div style={{marginTop: "1rem"}}>
                            <button onClick={handleSave} >
                                {editModal.shift ? "Saglabāt" : "Pievienot maiņu"}
                            
                            </button>
                            <button onClick={handleDelete} style={{marginLeft: "0.5rem"}}>
                                {editModal.shift ? "Dzēst maiņu" : "Atcelt"}
                            </button>
                            <button
                            onClick={() => setEditModal(null)}
                            style={{marginLeft: "0.5rem"}}
                            >
                                Aizvērt
                            </button>
                    </div>
                </div>
            </div>
            )}
        </div>
    );

}

export default SchedulePage;
