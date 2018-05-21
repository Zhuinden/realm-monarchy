# Monarchy 0.2.0 (2018-05-22)

- Added `Monarchy.findAllPagedWithChanges()` and `Monarchy.createDataSourceFactory()`.

- Added dependency on `api "android.arch.paging:runtime:1.0.0"` to support `findAllPagedWithChanges()`.

- Updated Realm to 5.1.0. Carried over breaking change that `OrderedCollectionChangeSet` is no longer nullable, and instead must check for `changeSet.getState() == State.INITIAL`. This is relevant in `managed` mode.

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