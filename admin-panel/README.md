# Admin Panel

A single self-contained page (`admin.html`) for managing the academy without going through the
Android app. It talks to the same Supabase project as the app, using the same public anon key
and the same Supabase Auth + REST API - nothing about the backend or its RLS policies changes
for this. It is not a Claude Artifact and has no build step; it's a plain HTML file.

## Who can use it

Only an account whose `profiles.role` is `ADMIN` or `DIRECTOR`. Sign in with the same
email/password you use in the app. If the account isn't an admin, it signs you back out and
tells you so.

## What it does

- **Students**: view roster, reassign a student's teacher, promote a student straight to
  Teacher, delete a profile.
- **Teachers**: view roster with assigned-student counts, promote to Admin, demote back to
  Student, delete a profile.
- **Classes**: view all classes with status, schedule a new one (always by picking a real
  registered student/teacher from a dropdown - never free-typed names), cancel or delete one.

Everything is enforced server-side by the RLS policies in `supabase/migrations` - this page is
just a UI over the same REST endpoints the app uses. It's not a separate trust boundary.

## Running it

It's a static file - no server, no build:

- **Locally**: just open `admin.html` in a browser.
- **A shareable link, in ~10 seconds, no signup**: go to https://app.netlify.com/drop and drag
  `admin.html` onto the page. You get a live URL immediately. Re-drag the file any time you
  update it to redeploy.
- **A permanent domain**: host it anywhere that serves static files (Netlify, Vercel, GitHub
  Pages, S3, your own server) - it's one HTML file with everything inlined, nothing else to
  configure.

## Notes

- Deleting a "profile" removes their academy record (name, role, assigned teacher, etc.), not
  their login - they can still sign in, they'll just get a fresh STUDENT profile provisioned
  automatically on next login (see the `handle_new_user` trigger in migration 0001).
- Promoting/demoting roles here works because an admin's own JWT satisfies
  `is_admin_or_director()` in the RLS policies, which is explicitly allowed to bypass the
  self-role-escalation trigger for *other* users' rows (see migration 0001's
  `prevent_role_self_escalation` trigger).
