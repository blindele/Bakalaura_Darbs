import "./App.css";

function App() {
  return (
    <div className="container">
      <header className="navbar">
        <h2>Plānotājs</h2>
        <button className="login-btn">Pieslēgties</button>
      </header>

      <main className="hero">
        <h1>Darba grafiku plānošanas sistēma</h1>
        <p>
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
        </p>

        <div className="buttons">
          <button className="primary">Sākt</button>
          <button className="secondary">Uzzināt vairāk</button>
        </div>
      </main>
    </div>
  );
}

export default App;