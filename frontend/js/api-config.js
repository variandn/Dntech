/* ============================================
   DNtech — Centralized API Configuration
   Change API_BASE when switching environments.
   ============================================ */

// Local development (Tomcat deploys WAR as ROOT.war → no context path)
const API_BASE = 'http://localhost:8080/api';

// Production example (update when deploying to Render / cloud):
// const API_BASE = 'https://your-backend.onrender.com/api';
