import { BrowserRouter, Route, Routes } from "react-router-dom";
import PublicLayout from "./layouts/PublicLayout";
import AppLayout from "./layouts/AppLayout";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import RegisterPsychologistPage from "./pages/RegisterPsychologistPage";
import RegisterClientPage from "./pages/RegisterClientPage";
import DashboardPage from "./pages/DashboardPage";
import ClientsPage from "./pages/ClientsPage";
import ClientDetailPage from "./pages/ClientDetailPage";
import SessionsPage from "./pages/SessionsPage";
import JournalPage from "./pages/JournalPage";
import RecommendationsPage from "./pages/RecommendationsPage";
import ChatPage from "./pages/ChatPage";
import InvitesPage from "./pages/InvitesPage";
import NotificationsPage from "./pages/NotificationsPage";
import ProfilePage from "./pages/ProfilePage";
import AdminPage from "./pages/AdminPage";
import NotFoundPage from "./pages/NotFoundPage";
import { ProtectedRoute } from "./components/ProtectedRoute";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<PublicLayout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register/psychologist" element={<RegisterPsychologistPage />} />
          <Route path="/register" element={<RegisterClientPage />} />
        </Route>

        <Route
          path="/app"
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<DashboardPage />} />
          <Route
            path="clients"
            element={
              <ProtectedRoute roles={["ROLE_PSYCHOLOGIST"]}>
                <ClientsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="clients/:id"
            element={
              <ProtectedRoute roles={["ROLE_PSYCHOLOGIST"]}>
                <ClientDetailPage />
              </ProtectedRoute>
            }
          />
          <Route path="sessions" element={<SessionsPage />} />
          <Route
            path="journal"
            element={
              <ProtectedRoute roles={["ROLE_CLIENT"]}>
                <JournalPage />
              </ProtectedRoute>
            }
          />
          <Route path="recommendations" element={<RecommendationsPage />} />
          <Route path="chat" element={<ChatPage />} />
          <Route
            path="invites"
            element={
              <ProtectedRoute roles={["ROLE_PSYCHOLOGIST"]}>
                <InvitesPage />
              </ProtectedRoute>
            }
          />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route
            path="admin"
            element={
              <ProtectedRoute roles={["ROLE_ADMIN"]}>
                <AdminPage />
              </ProtectedRoute>
            }
          />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
