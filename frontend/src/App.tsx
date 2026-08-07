import { BackgroundScene } from './components/BackgroundScene';
import { GroupOrderPage } from './components/GroupOrderPage';

export default function App() {
  return (
    <div className="app-shell">
      <BackgroundScene />
      {/* Dark semi-transparent contrast filter over the background art. */}
      <div className="app-backdrop-filter" aria-hidden="true" />

      <main className="app-container">
        <GroupOrderPage />
      </main>

      <footer className="app-container pt-0 pb-8 text-center text-xs text-stone-600">
        HPE Morpheus Coffee Club — everyone gets their turn.
      </footer>
    </div>
  );
}
