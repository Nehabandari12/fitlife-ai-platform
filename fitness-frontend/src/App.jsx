import { useContext, useEffect } from "react"
import { AuthContext } from "react-oauth2-code-pkce"
import { useDispatch } from "react-redux";
import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router"
import { logout, setCredentials } from "./store/authSlice";
import ActivityForm from "./components/ActivityForm";
import ActivityList from "./components/ActivityList";
import ActivityDetail from "./components/ActivityDetail";
import AllActivities from "./components/AllActivities";
import Navbar from "./components/Navbar";
import CompleteRecommendation from "./components/CompleteRecommendation";

const HomePage = ({ isAuthenticated }) => {
  if (!isAuthenticated) {
    return (
      <div className="flex items-center justify-center h-screen bg-gray-900">
        <div className="text-center">
          <h1 className="text-4xl font-bold mb-4 text-white">Welcome to FitLife</h1>
          <p className="text-xl text-gray-300">Please login to continue</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-900 py-8 px-4">
      <div className="max-w-6xl mx-auto bg-gray-800/50 rounded-2xl border border-gray-700 p-8 shadow-2xl">
        <h1 className="text-4xl font-bold text-white mb-8">Track Your Activities</h1>
        <ActivityForm onActivityAdded={() => window.location.reload()}/>
        <ActivityList limit={3} showSeeMore={true} />
      </div>
    </div>
  );
}

function App() {
  const { token, tokenData, logIn, logOut } = useContext(AuthContext);
  const dispatch = useDispatch();

  useEffect(() => {
    if (token) {
      dispatch(setCredentials({token, user: tokenData}));
    }
  }, [token, tokenData, dispatch]);

  const handleLogin = () => {
    console.log("Login button clicked");
    logIn();
  };

  const handleLogout = () => {
    console.log("Logout button clicked");
    dispatch(logout());
    logOut();
  };

  return (
    <Router>
      <Navbar 
        isAuthenticated={!!token}
        tokenData={tokenData}
        onLogin={handleLogin}
        onLogout={handleLogout}
      />
      
      <div className="pt-20">
        <Routes>
          <Route path="/" element={<HomePage isAuthenticated={!!token} />} />
          
          <Route path="/all-activities" element={
            token ? <AllActivities /> : <Navigate to="/" replace/>
          }/>
          
          <Route path="/complete-recommendation" element={
            token ? (
              <CompleteRecommendation 
                userId={tokenData?.sub}
                token={token}
              />
            ) : <Navigate to="/" replace/>
          }/>
          
          <Route path="/activities/:id" element={
            token ? (
              <div className="min-h-screen bg-gray-900 py-8 px-4">
                <ActivityDetail />
              </div>
            ) : <Navigate to="/" replace/>
          }/>
        </Routes>
      </div>
    </Router>
  )
}

export default App