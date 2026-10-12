# Payment Service (sandbox)

This service records payment attempts in PostgreSQL and provides a mock outcome flow. It does not contact a bank, wallet, or payment processor and must not be treated as production payment processing. In the order flow, clients create payments through `order-service`, which derives the amount and currency from its persisted order before making an authenticated internal request.

## Configuration

Set `JWT_SECRET_BASE64` to the same standard-Base64-encoded random secret used by `user-service` (at least 32 decoded bytes), plus matching `JWT_ISSUER_URI` and `JWT_AUDIENCE`. Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` for PostgreSQL. `PAYMENT_MOCK_ENABLED` enables the sandbox-only outcome endpoint; the application default is `false`, while the local Docker Compose configuration enables it.

The internal payment-creation endpoint accepts requests only from the service token whose subject is `order-service`; clients cannot choose an order's payable amount directly. Order, payment, inventory, and notification services must use the same JWT issuer, audience, and secret. Payment state callbacks are authenticated as `payment-service`; if a callback request fails, replaying the same mock outcome or cancellation retries the idempotent callback.

## API

Every public `/api/payments` endpoint requires a valid Bearer JWT whose subject is the user's UUID. Users can view only their own payments. Refunds require the `ROLE_ADMIN` authority. Payment creation is intentionally available through the authenticated order flow rather than a public payment endpoint.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/payments` | List the current user's payments using `page` and `size` pagination (maximum size 100). |
| `GET` | `/api/payments/{paymentId}` | Get a payment owned by the current user. |
| `GET` | `/api/payments/by-order/{orderId}` | Get the current user's payment for an order. |
| `POST` | `/api/payments/{paymentId}/simulate` | Sandbox-only transition from `PENDING` to `SUCCEEDED` (`{"outcome":"APPROVED"}`) or `FAILED` (`{"outcome":"DECLINED"}`). |
| `POST` | `/api/payments/{paymentId}/cancel` | Cancel a pending payment. |
| `POST` | `/api/payments/{paymentId}/refund` | Admin-only transition from `SUCCEEDED` to `REFUNDED`; this does not move real money. |

## Order checkout flow

1. Create an order with `POST /api/orders` using `items: [{ "productId": "...", "quantity": 1 }]`. The order service resolves product price and currency from `product-service` and reserves stock before returning the pending order.
2. Start payment with `POST /api/orders/{orderId}/payments` using `method` and an `idempotencyKey`. The order service supplies the persisted order total and currency to the payment service.
3. In this sandbox, simulate the outcome with `POST /api/payments/{paymentId}/simulate` and `{"outcome":"APPROVED"}` or `{"outcome":"DECLINED"}`. The payment service sends an authenticated event to the order service, which confirms or cancels the order and finalizes or releases its stock reservation.

Inventory reservation uses Redis locks plus PostgreSQL transactions and row locks. Notification events are persisted in PostgreSQL and queued in Redis; a delivery worker and real email/SMS/push provider are not included, so enqueueing does not mean a message has been delivered.

Health and readiness probes are available at `/actuator/health`.
