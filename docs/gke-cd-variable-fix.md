# GKE CD variable fix

The GKE CD workflow requires the Workload Identity provider value to use the real numeric Google Cloud project number.

Use this format:

```text
projects/123456789012/locations/global/workloadIdentityPools/github-actions/providers/github
```

Do not use placeholder values like `PROJECT_NUMBER` or `PROJECT_ID` in repository variables.
