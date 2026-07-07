# Frontend API Contract

The Spring Boot backend is the source of truth for the Next.js UI.

## Access rule

Protected UI calls go through the Next.js backend proxy. Browser components should call local protected paths and the proxy forwards to Spring Boot with the session token.

Public marketing calls use local public routes that forward to Spring Boot.

## Subscription payload

Marketing subscription forms should normalize into a subscriber payload with these fields:

- contact
- subscriberType
- preferredChannel
- affiliateRegistered
- name
- interest

Preferred subscriber types are USER, BUSINESS_OWNER, AFFILIATE, and DEV_HUB_CLIENT. Preferred channel is EMAIL unless the UI explicitly supports another channel.

## Dashboard UI rule

Dashboards must render real backend data and states, not endpoint labels as the main product screen.

Every dashboard route should show loading, error, empty, and live data states.

## Collection response shapes

The frontend should support both raw arrays and page-style responses with a content array.

## Core dashboard resources

- User dashboard and user business feed
- Products and product barcodes
- Transactions and withdrawals
- Tips and tip paid status
- Ticket events, ticket purchases, and ticket verification
- Promotions and registered subscriber promotion sends
- Billing plans, billing profile, and checkout sessions
- Reports and audit logs
- Affiliate referrals, commissions, payouts, and onboarding
