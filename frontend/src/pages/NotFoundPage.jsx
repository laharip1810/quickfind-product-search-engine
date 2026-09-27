import { Link } from 'react-router-dom';
import StateMessage from '../components/StateMessage.jsx';

export default function NotFoundPage() {
  return (
    <StateMessage title="Page not found" action={<Link className="btn" to="/">Go home</Link>}>
      The page you are looking for does not exist.
    </StateMessage>
  );
}
