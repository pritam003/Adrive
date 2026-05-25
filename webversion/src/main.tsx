import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import { ShareView } from './components/ShareView'
import { initUploadWorker } from './swUpload'
import './index.css'

const isShare = /^\/share\/[a-f0-9]{32}$/i.test(window.location.pathname);

// Register the background-upload service worker as soon as possible so
// in-flight uploads survive page reloads.
if (!isShare) initUploadWorker();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    {isShare ? <ShareView /> : <App />}
  </React.StrictMode>,
)
