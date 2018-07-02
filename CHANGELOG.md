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