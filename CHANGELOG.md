# Monarchy 1.0.0 (2020-10-10)
- BREAKING: Remove AndroidX Paging support. Due to the massive shift between Paging 2 and Paging 3, having the direct dependency on Paging is problematic.

Prefer frozen results instead, as it is more performant.

- Add support for `maxDepth` on copied results. (#11)

- Add some missing nullability annotations.

- Update Realm-Java to 7.0.8.

# Monarchy 0.7.1 (2020-06-25)
- Add support for `findAllFrozenWithChanges(Query)`.

- INTERNAL: the internal RealmResults refs are no longer snapshot collections, although this doesn't seem to have any external effects.

# Monarchy 0.7.0 (2020-06-25)
- Update to AndroidX and Paging 2.

- Update Realm to 7.0.0.

- Increase minSDK to 16 (to match Realm).

# Monarchy 0.5.1 (2018-10-08)
- Remove `createDataSourceFactory(Query, boolean asAsync)` because it doesn't actually work with Sync. :( 

- Fix `PagedLiveResults.updateQuery()`. Now it re-creates the RealmResults as it was originally intended.

# Monarchy 0.5.0 (2018-09-14)
- Add `openManually()` and `closeManually()` methods for people who use Sync and need their session to stay alive in a controlled manner. Please note that this increments the Realm ref count by one, and cannot be called multiple times. (So `openManually` called once, then `closeManually` called once).

- Add `createDataSourceFactory(Query, boolean asAsync)` to support creating a sync subscription through Paging. Please note that this is fairly experimental at this time.

# Monarchy 0.4.3 (2018-08-15)
- FIX: NPE when `updateQuery()` is called before a DataSource is created by DataSource.Factory

# Monarchy 0.4.2 (2018-07-27)
- CHANGE/FIX: `ManagedChangeSet` uses `setValue` only for the initial load of synchronous query, otherwise uses `postValue` to prevent `java.lang.IllegalStateException: Cannot call this method while RecyclerView is computing a layout or scrolling`

# Monarchy 0.4.1 (2018-07-25)
- CHANGE: `findAllManagedWithChanges(..., asAsync = false)` is replaced with `findAllManagedWithChangesSync(...)` (for better Kotlin support)
- FIX: `asAsync = false` was not working (whoops)

# Monarchy 0.4.0 (2018-07-23)

- BREAKING: `findAllSync` is renamed to `fetchAllManagedSync`
- Added `findAllManagedWithChanges(..., asAsync)` to allow synchronous evaluation of managed results.
- Internal change: `ManagedChangeSet` now uses `setValue` instead of `postValue`

# Monarchy 0.3.2 (2018-07-09)

- Fix that setting the new query should be atomic (and visible on the Monarchy thread).

# Monarchy 0.3.1 (2018-07-02)

- Made `Monarchy.RealmDataSourceFactory` public, and added `updateQuery(Query<T> query)` method.

# Monarchy 0.3.0 (2018-06-24)

- Added `Monarchy.fetchAllMappedSync()` and `Monarchy.fetchAllCopiedSync()` for those who desire to abandon all hope (in this case, change notifications). Common in DAO abstractions that ignore reactivity.

# Monarchy 0.2.2 (2018-05-23)

- Added counter-measures against a potential race condition when number of active subscriptions go from `1 -> 0` and `0 -> 1` rapidly.

# Monarchy 0.2.1 (2018-05-22)

- Added `Monarchy.findAllPagedWithChanges()` and `Monarchy.createDataSourceFactory()`.

- Added dependency on `api "android.arch.paging:runtime:1.0.0"` to support `findAllPagedWithChanges()`.

- Updated Realm to 5.1.0. Carried over breaking change that `OrderedCollectionChangeSet` is no longer nullable, and instead must check for `changeSet.getState() == State.INITIAL`. This is relevant in `managed` mode.

- Compared to 0.2.0, a crash was fixed that happened on back navigation + paged results.

# Monarchy 0.1.1 (2018-01-02)

- Javadocs.

- `startListening(LiveResults)` and `stopListening(LiveResults)` is no longer public, because it should have been package-private.

# Monarchy 0.1.0 (2017-12-21)

- Initial release of Monarchy.

- Ability to read and listen for copied RealmResults from a background looper thread as LiveData: `findAllCopiedWithChanges`

- Ability to read and listen for mapped RealmResults from a background looper thread as LiveData:: `findAllMappedWithChanges`

- Ability to read and listen for managed RealmResults from UI thread (with auto-managed Realm instance) as LiveData: `findAllManagedWithChanges`

- Synchronous read as list: `List<T> list = monarchy.findAllSync(realm, (realm) -> realm.where(...));` (internally, this returns managed objects in a snapshot collection)

- Realm blocks: `doWithRealm((realm) -> { ... })`

- Synchronous transactions: `runTransactionSync(transaction)`

- Async transactions: `writeAsync(transaction)`