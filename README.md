# ShopeeClone — Project Skeleton

A working Android app skeleton (Kotlin + Jetpack Compose + Supabase) modeled loosely on Shopee:
auth, product browsing, cart, checkout, order history, and a basic seller product-listing form.

## What's included
- **Auth**: Login / Signup screens wired to Supabase Auth (email/password)
- **Home**: Product grid with search (loads from a Postgres `products` table via Supabase, falls back to sample data)
- **Product Detail**: Add to cart / Buy now
- **Cart**: Quantity editing, remove items, total
- **Checkout**: Address + payment method (stub — no real payment gateway yet) → creates a row in `orders`
- **Order History**: Lists past orders for the logged-in user
- **Profile**: Logout, links to Order History and Seller Dashboard
- **Seller Dashboard**: Simple form to insert a new product row

Architecture: MVVM (ViewModel + mutableState, no external DI framework to keep it simple —
swap in Hilt later if the project grows).

## 1. Open the project
1. Install [Android Studio](https://developer.android.com/studio) (latest stable).
2. Open this folder (`ShopeeClone/`) as a project — "Open" → select the root folder.
3. Let Gradle sync (it will download dependencies — needs internet).

## 2. Connect Supabase (required for auth/data to actually work)
1. Go to [supabase.com](https://supabase.com) → create a free account and a new project (free tier: no card required, pauses after a week of inactivity but wakes back up on request).
2. In your project, go to **Settings → API** and copy the **Project URL** and **anon public key**.
3. Open `app/src/main/java/com/example/shopeeclone/data/remote/SupabaseClient.kt` and paste them in:
   ```kotlin
   private const val SUPABASE_URL = "https://YOUR_PROJECT_ID.supabase.co"
   private const val SUPABASE_ANON_KEY = "YOUR_ANON_KEY"
   ```
4. In **Authentication → Providers**, make sure **Email** is enabled (it is by default). For quick testing, you can disable "Confirm email" under Authentication → Settings so signups work immediately without an email link.
5. In the **SQL Editor**, run this to create the tables the app expects:
   ```sql
   create table users (
     uid uuid primary key,
     name text,
     email text,
     phone text default '',
     address text default '',
     is_seller boolean default false,
     profile_image_url text default ''
   );

   create table products (
     id uuid primary key,
     name text not null,
     description text default '',
     price numeric not null default 0,
     discount_price numeric,
     image_url text default '',
     category text default '',
     seller_id text default '',
     seller_name text default '',
     stock integer default 0,
     rating numeric default 0,
     sold_count integer default 0
   );

   create table orders (
     id uuid primary key,
     user_id text not null,
     items jsonb not null default '[]',
     total_amount numeric not null default 0,
     status text not null default 'PENDING',
     shipping_address text default '',
     created_at timestamptz default now()
   );

   -- Row Level Security: enable, then allow any logged-in user to read/write
   -- (fine for development — tighten before launch, e.g. restrict orders to their own user_id)
   alter table users enable row level security;
   alter table products enable row level security;
   alter table orders enable row level security;

   create policy "Allow authenticated read/write" on users for all using (auth.role() = 'authenticated');
   create policy "Allow public read, authenticated write" on products for select using (true);
   create policy "Allow authenticated write" on products for insert with check (auth.role() = 'authenticated');
   create policy "Allow authenticated read/write" on orders for all using (auth.role() = 'authenticated');
   ```
6. (Optional) Insert a few rows into `products` via the Table Editor to see real data instead of the built-in samples.

## 3. Run it
Click Run in Android Studio, or:
```
./gradlew installDebug
```
Without a real Supabase URL/key filled in, the app will still run and browse the 5 sample products
(no login will work until Supabase is connected).

## Known gaps / what to build next
- **Real payments**: Checkout currently just inserts a row into `orders`. Integrate Stripe
  (or a local provider) SDK when ready for real transactions — needs business verification.
- **Product images**: `imageUrl` field exists but upload/display isn't wired to Supabase Storage yet
  (create a `product-images` bucket in Storage, then upload from the Seller Dashboard).
- **Chat between buyer/seller**: not built.
- **Push notifications** (order updates): not built — Supabase doesn't include push out of the box;
  pair with Firebase Cloud Messaging or OneSignal for this piece specifically.
- **Reviews/ratings**: `rating` field exists on Product but there's no review submission UI.
- **Local cart persistence**: cart is in-memory only (`CartRepository`), clears on app restart.
  Swap for Room DB if you want it to survive app restarts.
- **Pagination**: product list loads everything at once — fine for a demo, add `.range()` calls
  to the Postgrest query once you have real product volume.
- **Row Level Security**: the SQL above is permissive for development. Before launch, restrict
  `orders` so users can only read their own rows (`user_id = auth.uid()`), and restrict `products`
  writes to the actual seller.

## Suggested next build order
1. Get Supabase connected and test login/signup
2. Add real product images (Storage upload in Seller Dashboard, Coil to display)
3. Wire local cart persistence (Room)
4. Add payments (Stripe test mode)
5. Push notifications for order status changes (via FCM or OneSignal)
6. Reviews & ratings
