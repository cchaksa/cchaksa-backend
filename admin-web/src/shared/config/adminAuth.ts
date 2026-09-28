export const adminAuthConfig = {
  signInPath: import.meta.env.VITE_ADMIN_SIGN_IN_PATH ?? '/api/admin/auth/signin',
} as const
