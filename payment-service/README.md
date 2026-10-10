# Payment Service (sandbox)

This service records payment attempts in PostgreSQL and provides a mock outcome flow. It does not contact a bank, wallet, or payment processor and must not be treated as production payment processing.

## Configuration

Set `JWT_SECRET_BASE64` to the same standard-Base64-encoded random secret used by `user-service` (at least 32 decoded bytes), plus matching `JWT_ISSUER_URI` and `JWT_AUDIENCE`. Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` for PostgreSQL. `PAYMENT_MOCK_ENABLED` enables the sandbox-only outcome endpoint; the application default is `false`, while the local Docker Compose configuration enables it.

The current order service does not expose an order-verification API. Consequently, the sandbox accepts an amount in the create request; a production integration must instead obtain the order owner, payable amount, and currency from a trusted order-service call before creating a payment.

## API

Every `/api/payments` endpoint requires a valid Bearer JWT. The JWT subject must be the user's UUID. Users can view only their own payments. Refunds require the `ROLE_ADMIN` authority.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/payments` | Create a `PENDING` payment. Request fields: `orderId`, positive `amount` (up to 2 decimal places), 3-letter `currency`, `method` (`CARD`, `BANK_TRANSFER`, `CASH_ON_DELIVERY`, or `WALLET`), and `idempotencyKey`. Replaying the same key and data returns the existing payment; changing data for that key returns `409`. |
| `GET` | `/api/payments` | List the current user's payments using `page` and `size` pagination (maximum size 100). |
| `GET` | `/api/payments/{paymentId}` | Get a payment owned by the current user. |
| `GET` | `/api/payments/by-order/{orderId}` | Get the current user's payment for an order. |
| `POST` | `/api/payments/{paymentId}/simulate` | Sandbox-only transition from `PENDING` to `SUCCEEDED` (`{"outcome":"APPROVED"}`) or `FAILED` (`{"outcome":"DECLINED"}`). |
| `POST` | `/api/payments/{paymentId}/cancel` | Cancel a pending payment. |
| `POST` | `/api/payments/{paymentId}/refund` | Admin-only transition from `SUCCEEDED` to `REFUNDED`; this does not move real money. |

Health and readiness probes are available at `/actuator/health`.
