import './App.css';
import { Row, Col } from 'react-bootstrap';
import Stock from './components/Stock';

function App() {
  return (
    <div className="App">
      <Row>
          <Stock></Stock>
      </Row>
    </div>
  );
}

export default App;
