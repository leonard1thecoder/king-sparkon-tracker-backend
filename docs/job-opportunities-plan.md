# Job Opportunities Feature Plan

## Goal
Owners can publish job posts, users and workers can maintain opportunity profiles, apply with resumes and optional certificates, and manage interview decisions from the Opportunities navigation area.

## Domain Design

### Job seeker profile
- One profile per tracker user.
- Captures highest qualification, one to five interested job titles, experience band, and an about section.
- First interested job is required; the next four are optional.
- Qualifications are constrained to: Grade 8, Grade 9, Grade 10, Grade 11, Grade 12, Higher Certificate, Diploma, Bachelor, Hons, Masters, and Dr.
- Experience is constrained to: less than one year, 1, 2, 3, 4, 5, and greater than five years.

### Job post
- Owned by a business owner and joined to the existing `businesses` table.
- Contains title, start date, closing date, description, required experience, uploaded job-post file URL, localized estimated salary, and currency.
- Currency follows existing localization country: South Africa uses ZAR, otherwise USD.
- Only open posts with closing date today or later are returned to workers/users.

### Application
- Joined to `job_posts`, `tracker_users`, and the profile snapshot.
- A user can apply once per job post.
- Resume URL is required; certificate URLs are optional.
- Owner view action changes status to `VIEWED`.
- Owner reject action changes status to `REJECTED`.
- Owner interview booking changes status to `INTERVIEW_BOOKED`.

### Interview
- Joined to application, post, business, and applicant.
- Owner books interview with interview date, description, expiration date, and business address copied from the existing business owner physical address.
- Applicant can accept or reject the interview while it is not expired.

## API Design

### Opportunities navigation
- `GET /api/opportunities` returns open jobs, my applications, and my interviews.
- `GET /api/opportunities/profile` returns my opportunity profile.
- `PUT /api/opportunities/profile` creates or updates my profile.
- `GET /api/opportunities/jobs` lists open jobs.
- `GET /api/opportunities/jobs/{jobPostId}` views one job post.
- `POST /api/opportunities/jobs/{jobPostId}/apply` applies for a job.
- `GET /api/opportunities/applications` lists my applications.
- `GET /api/opportunities/interviews` lists my interviews.
- `POST /api/opportunities/interviews/{interviewId}/accept` accepts an interview.
- `POST /api/opportunities/interviews/{interviewId}/reject` rejects an interview.

### Owner jobs
- `GET /api/owner/job-posts` lists owner business job posts.
- `POST /api/owner/job-posts` creates a job post.
- `GET /api/owner/job-posts/applications` lists applications for the owner business.
- `GET /api/owner/job-posts/{jobPostId}/applications` lists applications for one owner job post.
- `POST /api/owner/job-posts/applications/{applicationId}/view` marks an application viewed.
- `POST /api/owner/job-posts/applications/{applicationId}/reject` rejects an application.
- `POST /api/owner/job-posts/applications/{applicationId}/interview` books an interview.
- `GET /api/owner/job-posts/interviews` lists owner interviews.

## Email Design
- Application submitted: notify owner and applicant.
- Application rejected: notify applicant.
- Interview booked: notify applicant.
- Interview accepted/rejected: notify owner.
- Emails are non-blocking from the business transaction path; failures are logged and do not rollback job state.

## Test Plan
- Profile creation validates one to five job interests.
- Job creation validates owner role and closing date.
- Applying requires an existing profile and resume URL.
- Duplicate applications are rejected.
- Owner view changes application status to `VIEWED`.
- Booking an interview moves application status to `INTERVIEW_BOOKED` and creates a booked interview.
- Expired interviews cannot be accepted.
- Email hooks are verified for application submission and interview booking.
