# API endpoints: what each one does

The OpenAPI specs list the endpoints. They don't say which checks run on each one, or whether the
call reaches the bank. That differs from endpoint to endpoint, so this page spells it out.

## Key terms

- **Accelerator**: the WSO2 Open Banking component that owns the consents. Before it lets a request
  through, it can ask one of this repo's extensions to make a decision.
- **Bank backend**: holds the real account and payment data. It knows nothing about consents.
- **User token**: issued through the authorization code flow. It is tied to a consent the customer
  approved.
- **Client token**: issued through the client credentials flow. It is not tied to any consent.

## Policies

Every endpoint runs these API Manager policies, in this order:

1. **mTLS**
2. **JWT claim based access**
3. **Consent enforcement** *(only some endpoints)*: sends the request to `ValidateConsentAccessApi`,
   which checks it against the consent.
4. **Dynamic endpoint**: always last. It routes the call on to where it's handled.

The main difference between endpoints is whether **consent enforcement** runs. The tables below
show that for each one.

## Account Information API

Base path: `/open-banking/{version}/account-information`

| Endpoint | Token | Consent enforcement | Reaches bank backend |
|---|---|---|---|
| `GET /accounts` | User | ✅ | ✅ |
| `GET /accounts/{AccountId}` | User | ✅ | ✅ |
| `GET /accounts/{AccountId}/balances` | User | ✅ | ✅ |
| `GET /accounts/{AccountId}/transactions` | User | ✅ | ✅ |
| `GET /balances` | User | ✅ | ✅ |
| `GET /transactions` | User | ✅ | ✅ |
| `DELETE /consents/{ConsentId}` | **Client** | ❌ | ❌ |

**Why:**

- **The `GET` endpoints** return the customer's data. That needs the customer's permission (their
  consent), and the data lives in the bank.
- **`DELETE /consents/{ConsentId}`** acts *on* the consent, not *under* it, so there's nothing to
  enforce. `PreProcessConsentRevokeApi` handles it, and the bank backend isn't involved.

## Payment Initiation API

Base path: `/open-banking/{version}/payment-initiation`

| Endpoint | Token | Consent enforcement | Reaches bank backend |
|---|---|---|---|
| `POST /domestic-payments` | User | ✅ | ✅ |
| `GET /domestic-payments/{DomesticPaymentId}` | **Client** | ❌ | ✅ |
| `POST /domestic-scheduled-payments` | User | ✅ | ✅ |
| `GET /domestic-scheduled-payments/{DomesticScheduledPaymentId}` | **Client** | ❌ | ✅ |
| `POST /domestic-standing-orders` | User | ✅ | ✅ |
| `GET /domestic-standing-orders/{DomesticStandingOrderId}` | **Client** | ❌ | ✅ |
| `POST /international-payments` | User | ✅ | ✅ |
| `GET /international-payments/{InternationalPaymentId}` | **Client** | ❌ | ✅ |

**Why:**

- **`POST` (submit a payment):** the request is checked against the consent before it goes to the
  bank. The consent must still be `Authorised`, the `ConsentId` must match, and the `Initiation` and
  `Risk` blocks must match what the customer authorized, field for field. See
  `PaymentConsentValidatorUtil`.
- **`GET` (check a payment's status):** these calls use a client token, which has no consent tied
  to it, so there's nothing to enforce against.

### Endpoints that don't exist, on purpose

A payment consent is used up when the payment is submitted. As a result, two endpoints are left out:

- **No consent `DELETE`.** Once the payment is submitted there's nothing long-lived left to revoke.
  That's why `PreProcessConsentRevokeApiImpl` rejects payment consent types.
- **No funds confirmation.** Funds have to be checked when the payment is submitted anyway, so a
  separate funds check isn't offered. That check is up to the bank.
