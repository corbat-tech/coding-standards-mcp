/**
 * Home Page - Redirects to Posts
 */

import { redirect } from 'next/navigation';

export default function Home() {
  redirect('/posts');
}
