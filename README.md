# Monarchy

Wrapper over Realm, using Android Architecture Components.

Alpha version.

# How does it work?

1.) Initialize the Monarchy, which is basically a singleton wrapper around Realm

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
                
2.) create queries as LiveData, and observe them

``` java
Monarchy monarchy = application.getMonarchy();
monarchy.findAllWithChanges(realm -> realm.where(RealmDog.class))
        .observe(this, dogs -> {...});
```
        
3.) You can also create a Mapper which will map the RealmObject to something else

``` java
monarchy.findAllWithChanges(realm -> realm.where(RealmDog.class), dog -> Dog.create(dog.getName()))
        .observe(this, dogs -> {...});
```

4.) Instead of using `Realm.getDefaultInstance()` and `close()`, now you should just do

``` java
monarchy.doWithRealm((realm) -> {
    ....
});
```

Listening for copied/mapped results happens on a background looper thread.

Listening for managed results happens on the UI thread.

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
