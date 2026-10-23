// src/components/HelpModal.jsx
// Basic help for students (placeholder text until the team writes the final version).

import "./HelpModal.css";

const QUESTIONS = [
  {
    q: "How do I order?",
    a: "Browse the menu, tap Add to Cart, then open your cart and tap Place Order. You'll get an order number straight away.",
  },
  {
    q: "How do I pay?",
    a: "You pay at the cafeteria pay station when you collect your order. Show your order number or QR code to the cashier.",
  },
  {
    q: "When can I order?",
    a: "Orders can only be placed while the cafeteria is open. The banner at the top of the menu shows today's opening times.",
  },
  {
    q: "How do I know when my order is ready?",
    a: "Open My Orders. Your order moves from Submitted to Preparing to Ready. Collect it once it says Ready.",
  },
  {
    q: "Can I cancel an order?",
    a: "Yes, while it still says Submitted. Once the kitchen starts preparing it, please speak to the cafeteria staff.",
  },
  {
    q: "Something isn't working",
    a: "Try logging out and back in. If it still doesn't work, please ask the cafeteria staff or contact IT support.",
  },
];

function HelpModal({ onClose }) {
  return (
    <div className="help-overlay" onClick={onClose}>
      <div
        className="help-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="help-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="help-header">
          <h2 id="help-title">Help</h2>

          <button className="help-close" onClick={onClose} aria-label="Close help">
            ×
          </button>
        </div>

        <div className="help-body">
          {QUESTIONS.map((item) => (
            <details key={item.q} className="help-item">
              <summary>{item.q}</summary>
              <p>{item.a}</p>
            </details>
          ))}
        </div>
      </div>
    </div>
  );
}

export default HelpModal;
