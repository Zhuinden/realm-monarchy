# Monarchy

A wrapper over Realm, that exposes RealmResults as various forms of LiveData.

With that, you can use a singleton Monarchy instance to manage Realm queries, and possibly make it easier to hide Realm as implementation of the data layer.

# Adding to project

To use `Monarchy`, you need to add as a dependency:

    implementation 'com.github.Zhuinden:realm-monarchy:0.4.3'
    
And it's available on Jitpack, so you need to add

``` groovy
allprojects {
    repositories {
        // ...
        maven { url "https://jitpack.io" }
    }
    // ...
}
```

# How can I use it?

## Initialization

Initialize `Monarchy`, and create a new Monarchy instance (for a given RealmConfiguration):

``` java
Monarchy.init(this); // need to call this only once
monarchy = new Monarchy.Builder()
    .setRealmConfiguration(new RealmConfiguration.Builder()
         .deleteRealmIfMigrationNeeded()
         .initialData(realm -> {
              RealmDog dog = realm.createObject(RealmDog.class);
              dog.setName("Corgi");
         }).build()
    ).build();
```

## Queries
                
Create queries as LiveData, and observe them

``` java
LiveData<List<RealmDog>> dogs = monarchy.findAllCopiedWithChanges(realm -> realm.where(RealmDog.class));
dogs.observe(this, dogs -> {...});
```
        
You can also create a Mapper which will map the RealmObject to something else

``` java
LiveData<List<Dog>> dogs = monarchy.findAllMappedWithChanges(realm -> realm.where(RealmDog.class), dog -> Dog.create(dog.getName()));
dogs.observe(this, dogs -> {...});
```

Using the Android Architecture Components: Paging Library, you can now also obtain the RealmResults as a `LiveData<PagedList<T>>`.

``` java
DataSource.Factory<Integer, RealmDog> realmDataSourceFactory = monarchy.createDataSourceFactory(realm -> realm.where(RealmDog.class));
DataSource.Factory<Integer, Dog> dataSourceFactory = realmDataSourceFactory.map(input -> Dog.create(input.getName()));
LiveData<PagedList<Dog>> dogs = monarchy.findAllPagedWithChanges(realmDataSourceFactory,
                                                new LivePagedListBuilder<>(dataSourceFactory, 20));
dogs.observe(this, dogs -> {...});
```

You can also synchronously get mapped/copied results, but it's generally not recommended; it is preferred to find results with changes instead.

## Writes

You can do either synchronous transaction, or have the transaction be executed asynchronously on a dedicated single-threaded pool.

For synchronous transaction, you can use `runTransactionSync(Realm.Transaction)`.

``` java
monarchy.runTransactionSync(realm -> {
    RealmDog dog = realm.createObject(RealmDog.class);
    dog.setName("Doge");
});
```

For asynchronous transaction, you can use `writeAsync(Realm.Transaction)`.

``` java
monarchy.writeAsync(realm -> {
    RealmDog dog = realm.createObject(RealmDog.class);
    dog.setName("Doge");
});
```

## Working with Realm instances

Instead of using `Realm.getDefaultInstance()` and `close()`, now you should just do

``` java
monarchy.doWithRealm((realm) -> {
    ....
});
```

And otherwise expose the queries as LiveData, and observe them. Whether a LiveData has observers or not will properly manage the Realm lifecycle.

# Information

Listening for copied/mapped results happens on the background looper thread.

Listening for paged results happens on the background looper thread.

Listening for managed results happens on the UI thread.

# Possible FAQs

## Why isn't this written with Rx operators?
 
Because managing ref counting and doing specific callbacks when ref-counting is tricky, while LiveData makes it trivial.

``` java
Observable.just(5)
          .replay(1)
          .autoConnect(0)
          .doOnSubscribe(/* onActive */)
          .doOnUnsubscribe(/* onInactive */)
          .subscribe()
```

or something like that. Tricky stuff. So if you need LiveData exposed to Rx, then just use:

``` java
Flowable<List<Dog>> dogs = Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(lifecycleOwner, liveData));
```

with the help of 

```
implementation "android.arch.lifecycle:reactivestreams:1.1.1"
```

## How do I open a Realm instance and close it manually, without being in a block?

This is where most errors in Realm usage come from, so I specifically did not add an `open()`/`close()` method to `Monarchy`.

Realm already manages a reference counted cache for thread-local Realm instances, where of course `getInstance()` increases ref count. So if that doesn't suit you, feel free to keep your own `ThreadLocal<Realm>` cache.

## When should I compact the Realm?

Probably when you've finished every Activity. When's that? If you have only 1 finishing Activity, then it's easy, if you have more Activities, then that's a different problem :D

## How should I do schema migrations?

[`RealmAutoMigration`](https://github.com/Zhuinden/realm-helpers/blob/872233b7026546323259d4d608adce6915d53b0c/realm-auto-migration/src/main/java/com/zhuinden/realmautomigration/RealmAutoMigration.java) can help. 

Once you're done migrating your current fields to new fields, it can match the fields and migrate from one schema to another, removing any mismatched properties (or adding the new ones).

## Why is this library possible?

Because `LiveData` from the Android Architecture Components is the best thing since sliced bread.

## License

    Copyright 2017 Gabor Varadi

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
