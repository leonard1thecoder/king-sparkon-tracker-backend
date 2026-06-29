# Google Storage image setup

King Sparkon Tracker should store application pictures in Google Cloud Storage instead of keeping local files or uncontrolled external image URLs.

## Environment keys

Use these keys in `.env`, Cloud Run, or your secret/config manager:

```properties
APP_GOOGLE_STORAGE_ENABLED=true
APP_GOOGLE_STORAGE_PROJECT_ID=your-google-cloud-project-id
APP_GOOGLE_STORAGE_BUCKET_NAME=king-sparkon-tracker-images
APP_GOOGLE_STORAGE_PUBLIC_BASE_URL=https://storage.googleapis.com/king-sparkon-tracker-images
APP_GOOGLE_STORAGE_ROOT_FOLDER=king-sparkon-tracker
APP_GOOGLE_STORAGE_MAKE_PUBLIC=false
APP_GOOGLE_STORAGE_REJECT_EXTERNAL_IMAGE_URLS=true
APP_GOOGLE_STORAGE_MAX_FILE_SIZE_BYTES=5242880
```

For local development, point the Google client library to your service-account JSON file:

```properties
GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/service-account.json
```

For Cloud Run, prefer the runtime service account instead of a JSON file. Grant that service account permission to create objects in the image bucket.

## Upload endpoint

Authenticated clients can upload pictures with multipart form data:

```http
POST /api/v1/pictures
Content-Type: multipart/form-data

picture=@poster.png
folder=tickets/posters
```

Response:

```json
{
  "objectName": "king-sparkon-tracker/tickets/posters/owner/uuid.png",
  "url": "https://storage.googleapis.com/king-sparkon-tracker-images/king-sparkon-tracker/tickets/posters/owner/uuid.png",
  "contentType": "image/png",
  "sizeBytes": 120000
}
```

Save the returned `url` into fields such as `profilePictureUrl`, `bannerUrl`, and `posterPhotoUrl`.

## Current wiring

- Profile onboarding image values pass through `GoogleStorageService`.
- Base64 `data:image/*` values are uploaded into Google Storage.
- Existing Google Storage URLs are accepted as already stored.
- External HTTP image URLs are rejected when `APP_GOOGLE_STORAGE_ENABLED=true` and `APP_GOOGLE_STORAGE_REJECT_EXTERNAL_IMAGE_URLS=true`.
- Direct multipart uploads are available through `/api/v1/pictures`.

## Frontend rule

Upload the image first, then send the returned Google Storage URL in the normal create/update payload. Do not send random CDN, Instagram, Facebook, or temporary browser blob URLs into backend domain objects.
